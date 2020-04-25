import syntaxtree.*;
import visitor.GJDepthFirst;

public class FillTableVisitor extends GJDepthFirst<String, SymbolTable>{
    private String current_class;
    private String current_method;
    private boolean class_var;


    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    public String visit(MainClass n, SymbolTable symbol_table) {
        String main_class_name = n.f1.accept(this, symbol_table);
        if (!symbol_table.newClass(main_class_name, null, true)) {
            System.err.println("Class " + main_class_name + " already exists!");
            System.exit(1);
        }

        current_class = main_class_name;
        current_method = "main";
        class_var = false;

        if (!symbol_table.addMethod(main_class_name, "main", "void")) {
            System.err.println("Error while adding main methond in main class");
            System.exit(1);
        }

        //n.f11.accept(this, argu);
        n.f14.accept(this, symbol_table);
        n.f15.accept(this, symbol_table);
        return null;
    }


    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    public String visit(ClassDeclaration n, SymbolTable symbol_table){
        String classname = n.f1.accept(this, symbol_table);

        if (!symbol_table.newClass(classname, null, false)) {
            System.err.println("Class " + classname + " already exists!");
            System.exit(1);
        }

        current_class = classname;
        class_var = true;
        n.f3.accept(this, symbol_table);
        n.f4.accept(this, symbol_table);
        return null;
    }


    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    public String visit(ClassExtendsDeclaration n, SymbolTable symbol_table){
        String classname = n.f1.accept(this, symbol_table);
        String parentclass = n.f3.accept(this, symbol_table);

        if (!symbol_table.containsClass(parentclass)) {
            System.err.println("Parent class " + parentclass + " doesn't exist!");
            System.exit(1);
        }
        if (!symbol_table.newClass(classname, parentclass, false)) {
            System.err.println("Class " + classname + " already exists!");
            System.exit(1);
        }

        current_class = classname;
        class_var = true;

        n.f5.accept(this, symbol_table);
        n.f6.accept(this, symbol_table);
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public String visit(VarDeclaration n, SymbolTable symbol_table) {
        String type = n.f0.accept(this, symbol_table);
        String var = n.f1.accept(this, symbol_table);

        if (class_var) {
            if (!symbol_table.addVar (current_class, type, var)){
                System.err.println("Error while adding variable " + var + " in class " + current_class);
                System.exit(1);
            }
        }
        else {
            if (!symbol_table.addVar (current_class, current_method, type, var)) {
                System.err.println("Error while adding variable " + var + " in method " + current_method);
                System.exit(1);
            }
        }

        return null;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    public String visit(MethodDeclaration n, SymbolTable symbol_table){
        String type = n.f1.accept(this, symbol_table);
        String method_name = n.f2.accept(this, symbol_table);

        //System.out.println(type + ' ' + method_name + ' ' + current_class);

        if (!symbol_table.addMethod(current_class, method_name, type)) {
            System.err.println("Error while adding method " + method_name + " in class " + current_class);
            System.exit(1);
        }
        current_method = method_name;

        class_var = false;

        n.f4.accept(this, symbol_table);

        //System.out.println("current_class = " + current_class);

        if (!symbol_table.overrideCheck (current_class, method_name, type)){
            System.err.println("Error while adding overriding method " + method_name + " in class " + current_class);
            System.exit(1);
        }

        n.f7.accept(this, symbol_table);
        n.f8.accept(this, symbol_table);
        n.f10.accept(this, symbol_table);
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public String visit(FormalParameter n, SymbolTable symbol_table) {
        String type = n.f0.accept(this, symbol_table);
        String param = n.f1.accept(this, symbol_table);

        //System.out.println('\t' + type + ' ' + var);

        if (!symbol_table.addParameters (current_class, current_method, type, param)) {
            System.err.println("Error encountered while adding parameter " + param + " in method " + current_method);
            System.exit(1);
        }
        return null;
    }

    /**
     * f0 -> "boolean"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(BooleanArrayType n, SymbolTable symbol_table) {
        return "boolean[]";
    }


    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(IntegerArrayType n, SymbolTable symbol_table) {
        return "int[]";
    }

    /**
     * f0 -> "boolean"
     */
    public String visit(BooleanType n, SymbolTable symbol_table) {
        return "boolean";
    }

    /**
     * f0 -> "int"
     */
    public String visit(IntegerType n, SymbolTable symbol_table) {
        return "int";
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Identifier n, SymbolTable symbol_table) {
        return n.f0.toString();
    }

}
