package visitors;

import syntaxtree.*;
import visitor.GJDepthFirst;
import java.io.*;
import java.lang.String;
import types.*;
public class LoweringVisitor extends GJDepthFirst<String, SymbolTable> {
    private String current_class;
    private String current_method;
    private final PrintWriter pw;
    private int current_reg_num;
    private int current_if_label_num;

    public LoweringVisitor(String current_input) throws Exception {
        String path = current_input.substring(0, current_input.length()-5);
        File file = new File(path + ".ll");
        pw = new PrintWriter(file);
        current_reg_num = 0;
    }

    private String newTemp() {
       return "%_" + current_reg_num++;
    }

    private String newIfLabel() {
        return "if" + current_if_label_num++;
    }

    private static boolean isNumeric(String str) {
        if (str==null) return false;
        try {
            Integer.parseInt(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }

    void emit (String buf_output) {
        pw.println(buf_output);
    }

    String getLLVMType (String java_type) throws Exception {
        switch (java_type) {
            case "int":
                return "i32";
            case "boolean":
                return "i1";
            default:
                throw new Exception ("cannot determine type " + java_type);
        }
    }

    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    public String visit(Goal n, SymbolTable symbol_table) throws Exception {
        n.f0.accept(this, symbol_table);
        n.f1.accept(this, symbol_table);
        n.f2.accept(this, symbol_table);

        pw.close();
        return null;
    }


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
    public String visit(MainClass n, SymbolTable symbol_table) throws Exception {
        String main_class_name = n.f1.accept(this, symbol_table);
        current_class = main_class_name;
        current_method = "main";


        emit ("@." + main_class_name + "_vtable = global [0 x i8*] []\n");

        emit ("declare i8* @calloc(i32, i32)\n" +
                "declare i32 @printf(i8*, ...)\n" +
                "declare void @exit(i32)\n" +
                "\n" +
                "@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n" +
                "@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n" +
                "define void @print_int(i32 %i) {\n" +
                "\t%_str = bitcast [4 x i8]* @_cint to i8*\n" +
                "\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n" +
                "\tret void\n" +
                "}\n" +
                "\n" +
                "define void @throw_oob() {\n" +
                "\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n" +
                "\tcall i32 (i8*, ...) @printf(i8* %_str)\n" +
                "\tcall void @exit(i32 1)\n" +
                "\tret void\n" +
                "}\n");

        emit("define i32 @main() {");

        n.f11.accept(this, symbol_table);
        n.f14.accept(this, symbol_table);
        n.f15.accept(this, symbol_table);

        emit("\tret i32 0");

        emit("}");
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public String visit(VarDeclaration n, SymbolTable symbol_table) throws Exception {
        String type = n.f0.accept(this, symbol_table);
        String id = n.f1.accept(this, symbol_table);
        emit("\t%" + id + " = alloca " + type);
        return null;
    }

    //%val = load i32, i32* %ptr
    //emit("\tload i32, i32* " + );
//
//    /**
//     * f0 -> "class"
//     * f1 -> Identifier()
//     * f2 -> "{"
//     * f3 -> ( VarDeclaration() )*
//     * f4 -> ( MethodDeclaration() )*
//     * f5 -> "}"
//     */
//    public String visit(ClassDeclaration n, SymbolTable symbol_table) throws Exception {
//        String classname = n.f1.accept(this, symbol_table);
//
//        current_class = classname;
//        n.f3.accept(this, symbol_table);
//        n.f4.accept(this, symbol_table);
//        return null;
//    }
//
//
//    /**
//     * f0 -> "class"
//     * f1 -> Identifier()
//     * f2 -> "extends"
//     * f3 -> Identifier()
//     * f4 -> "{"
//     * f5 -> ( VarDeclaration() )*
//     * f6 -> ( MethodDeclaration() )*
//     * f7 -> "}"
//     */
//    public String visit(ClassExtendsDeclaration n, SymbolTable symbol_table) throws Exception {
//        String classname = n.f1.accept(this, symbol_table);
//        String parentclass = n.f3.accept(this, symbol_table);
//
//        current_class = classname;
//
//        n.f5.accept(this, symbol_table);
//        n.f6.accept(this, symbol_table);
//        return null;
//    }
//
//    /**
//     * f0 -> "public"
//     * f1 -> Type()
//     * f2 -> Identifier()
//     * f3 -> "("
//     * f4 -> ( FormalParameterList() )?
//     * f5 -> ")"
//     * f6 -> "{"
//     * f7 -> ( VarDeclaration() )*
//     * f8 -> ( Statement() )*
//     * f9 -> "return"
//     * f10 -> Expression()
//     * f11 -> ";"
//     * f12 -> "}"
//     */
//    public String visit(MethodDeclaration n, SymbolTable symbol_table) throws Exception {
//        String declaration_return_type = n.f1.accept(this, symbol_table);
//        String method_name = n.f2.accept(this, symbol_table);
//        current_method = method_name;
//
//        n.f4.accept(this, symbol_table);
//
//        n.f7.accept(this, symbol_table);
//        n.f8.accept(this, symbol_table);
//
//        String actual_return_type = n.f10.accept(this, symbol_table);
//        if (!actual_return_type.equals(declaration_return_type) && !symbol_table.isParentType(actual_return_type, declaration_return_type))
//            throw new Exception("Method " + method_name + " returns " + actual_return_type + ", while is declared to return " + declaration_return_type);
//        return null;
//    }
//
//    /**
//     * f0 -> Type()
//     * f1 -> Identifier()
//     */
//    public String visit(FormalParameter n, SymbolTable symbol_table) throws Exception {
//        String type = n.f0.accept(this, symbol_table);
//        String param = n.f1.accept(this, symbol_table);
//
//        if (!type.equals("int") && !type.equals("boolean") && !type.equals("int[]") && !type.equals("boolean[]") && !symbol_table.containsClass(type))
//            throw new Exception("Unknown type " + type + " of var " + param);

//        return null;
//    }
//
//    /**
//     * f0 -> "boolean"
//     * f1 -> "["
//     * f2 -> "]"
//     */
//    public String visit(BooleanArrayType n, SymbolTable symbol_table) throws Exception {
//        return "boolean[]";
//        return null;
//    }
//
//    /**
//     * f0 -> "int"
//     * f1 -> "["
//     * f2 -> "]"
//     */
//    public String visit(IntegerArrayType n, SymbolTable symbol_table) throws Exception {
//        //return "int[]";
//        return null;
//    }
//
//    /**
//     * f0 -> "boolean"
//     */
//    public String visit(BooleanType n, SymbolTable symbol_table) throws Exception {
//        //return "boolean";
//        return null;
//    }

    /**
     * f0 -> "int"
     */
    public String visit(IntegerType n, SymbolTable symbol_table) throws Exception {
        return "i32";
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Identifier n, SymbolTable symbol_table) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public String visit(AssignmentStatement n, SymbolTable symbol_table) throws Exception {
        String id = n.f0.accept(this, symbol_table);
        String java_type = symbol_table.getTypeofIdentifier(id, current_class, current_method);
        String llvm_type = getLLVMType(java_type);
        System.out.println("llvm type of " + id + " with java type = " + java_type + " = " + llvm_type);
        String expr = n.f2.accept(this, symbol_table);
        System.out.println ("expr = " + expr);

        //store i32 %val, i32* %ptr
        emit("\tstore " + llvm_type + " " + expr + ", i32* %" + id);

        return null;
    }


//    /**
//     * f0 -> Identifier()
//     * f1 -> "["
//     * f2 -> Expression()
//     * f3 -> "]"
//     * f4 -> "="
//     * f5 -> Expression()
//     * f6 -> ";"
//     */
//    public String visit(ArrayAssignmentStatement n, SymbolTable symbol_table) throws Exception {
//        String expr_type = n.f2.accept(this, symbol_table);
//        if (!expr_type.equals("int")) throw new Exception("Array intex type is " + expr_type + ", not int");
//
//        String id = n.f0.accept(this, symbol_table);
//
//        expr_type = n.f5.accept(this, symbol_table);
//        if (expr_type == null) throw new Exception("Expression type in assignment is null");
//
//        String id_type = symbol_table.getTypeofIdentifier(id, current_class, current_method);
//        if (id_type.equals("") || id_type == null)
//            throw new Exception("Cannot determine the type of identifier " + id);
//        else if (id_type.equals("int[]")) {
//            if (!expr_type.equals("int"))
//                throw new Exception("Cannot assign " + expr_type + " to " + id_type + ". Assignment is not valid.");
//        }else if (id_type.equals("boolean[]")) {
//            if (!expr_type.equals("boolean"))
//                throw new Exception("Cannot assign " + expr_type + " to " + id_type + ". Assignment is not valid.");
//        }else throw new Exception("Cannot assign " + expr_type + " to " + id_type + ". Assignment is not valid.");
//        return null;
//    }


    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    public String visit(IfStatement n, SymbolTable symbol_table) throws Exception {
        String expression = n.f2.accept(this, symbol_table);

        String if_label = newIfLabel();
        String else_label = newIfLabel();
        String end_label = newIfLabel();

        //br i1 %case, label %if, label %else
        emit("\tbr i1 " + expression + ", label %" + if_label + ", label %" + else_label);

        emit("\n\t" + if_label + ":");
        n.f4.accept(this, symbol_table);
        //br label %goto
        emit("\tbr label %" + end_label);

        emit("\n\t" + else_label + ":");
        n.f6.accept(this, symbol_table);
        //br label %goto
        emit("\tbr label %" + end_label);

        emit("\n\t" + end_label + ":");
        return null;
    }
//
//    /**
//     * f0 -> "while"
//     * f1 -> "("
//     * f2 -> Expression()
//     * f3 -> ")"
//     * f4 -> Statement()
//     */
//    public String visit(WhileStatement n, SymbolTable symbol_table) throws Exception {
//        String expression = n.f2.accept(this, symbol_table);
//
//        if (!expression.equals("boolean")) throw new Exception("Expected boolean type for while expression, but got " + expression);
//        n.f4.accept(this, symbol_table);
//        return null;
//    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public String visit(PrintStatement n, SymbolTable symbol_table) throws Exception {
        String expr = n.f2.accept(this, symbol_table);
        //call void (i32) @print_int(i32 %_0)
        emit("\tcall void (i32) @print_int(i32 " + expr + ")");
        return null;
    }

//    /**
//     * f0 -> Clause()
//     * f1 -> "&&"
//     * f2 -> Clause()
//     */
//    public String visit(AndExpression n, SymbolTable symbol_table) throws Exception {
//        String clause1 = n.f0.accept(this, symbol_table);
//        String clause2 = n.f2.accept(this, symbol_table);
//
//        if (!clause1.equals("boolean") || !clause2.equals("boolean"))
//            throw new Exception("Cannot apply && operator between " + clause1 + " and " + clause2);
//        return "boolean";
//        return null;
//    }


//    /**
//     * f0 -> "!"
//     * f1 -> Clause()
//     */
//    public String visit(NotExpression n, SymbolTable symbol_table) throws Exception {
//        String clause = n.f1.accept(this, symbol_table);
//        if (!clause.equals("boolean")) throw new Exception("Expected boolean type for logical not operator, but got " + clause);
//        return "boolean";
//        return null;
//    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public String visit(CompareExpression n, SymbolTable symbol_table) throws Exception {
        String pr_expr1 = n.f0.accept(this, symbol_table);
        String pr_expr2 = n.f2.accept(this, symbol_table);

        String temp_reg = newTemp();

        //%case = icmp slt i32 %a, %b
        emit("\t" + temp_reg + " = icmp slt i32 " + pr_expr1 + ", " + pr_expr2);
        return temp_reg;
    }


//    /**
//     * f0 -> PrimaryExpression()
//     * f1 -> "+"
//     * f2 -> PrimaryExpression()
//     */
//    public String visit(PlusExpression n, SymbolTable symbol_table) throws Exception {
//        String pr_expr1 = n.f0.accept(this, symbol_table);
//        String pr_expr2 = n.f2.accept(this, symbol_table);
//
//        if (!pr_expr1.equals("int") || !pr_expr2.equals("int"))
//            throw new Exception("Cannot apply + operator between " + pr_expr1 + " and " + pr_expr2);
//        return "int";
//        return null;
//    }

//    /**
//     * f0 -> PrimaryExpression()
//     * f1 -> "-"
//     * f2 -> PrimaryExpression()
//     */
//    public String visit(MinusExpression n, SymbolTable symbol_table) throws Exception {
//        String pr_expr1 = n.f0.accept(this, symbol_table);
//        String pr_expr2 = n.f2.accept(this, symbol_table);
//        if (!pr_expr1.equals("int") || !pr_expr2.equals("int"))
//            throw new Exception("Cannot apply - operator between " + pr_expr1 + " and " + pr_expr2);
//        return "int";
//        return null;
//    }


//    /**
//     * f0 -> PrimaryExpression()
//     * f1 -> "*"
//     * f2 -> PrimaryExpression()
//     */
//    public String visit(TimesExpression n, SymbolTable symbol_table) throws Exception {
////        String pr_expr1 = n.f0.accept(this, symbol_table);
////
////        String pr_expr2 = n.f2.accept(this, symbol_table);
////        if (!pr_expr1.equals("int") || !pr_expr2.equals("int"))
////            throw new Exception("Cannot apply * operator between " + pr_expr1 + " and " + pr_expr2);
////
////        return "int";
//        return null;
//    }

//
//    /**
//     * f0 -> PrimaryExpression()
//     * f1 -> "["
//     * f2 -> PrimaryExpression()
//     * f3 -> "]"
//     */
//    public String visit(ArrayLookup n, SymbolTable symbol_table) throws Exception {
////        String pr_expr_type = n.f2.accept(this, symbol_table);
////        if (!pr_expr_type.equals("int")) throw new Exception("Array intex type is " + pr_expr_type + ", not int");
////
////        pr_expr_type = n.f0.accept(this, symbol_table);
////        if (pr_expr_type.equals("int[]")) return "int";
////        else if (pr_expr_type.equals("boolean[]")) return "boolean";
////        else throw new Exception("Cannot apply array lookup to " + pr_expr_type);
//        return null;
//    }

//    /**
//     * f0 -> PrimaryExpression()
//     * f1 -> "."
//     * f2 -> "length"
//     */
//    public String visit(ArrayLength n, SymbolTable symbol_table) throws Exception {
////        String pr_expr_type = n.f0.accept(this, symbol_table);
////        if (!pr_expr_type.equals("int[]") && !pr_expr_type.equals("boolean[]"))
////            throw new Exception("Cannot apply .length to " + pr_expr_type);
////        return "int";
//        return null;
//    }
//
//
//    /**
//     * f0 -> PrimaryExpression()
//     * f1 -> "."
//     * f2 -> Identifier()
//     * f3 -> "("
//     * f4 -> ( ExpressionList() )?
//     * f5 -> ")"
//     */
//    public String visit(MessageSend n, SymbolTable symbol_table) throws Exception {
//        String pr_expr = n.f0.accept(this, symbol_table);
//        String id = n.f2.accept(this, symbol_table);
//
//        MethodContents method_contents = symbol_table.getMethodContents (pr_expr, id);
//        if (method_contents==null) throw new Exception("Cannot get method " + id + " contents");
//        LinkedList<String> parameter_types = method_contents.getParameterTypes();
//
//        n.f4.accept(this, symbol_table);
//        LinkedList<String> argument_list = new LinkedList<String>();
//
//        if (!argument_stack.empty() && !argument_stack.peek().equals("(")) {
//            do argument_list.addFirst(argument_stack.pop());
//            while (!argument_stack.peek().equals("("));
//            String lparen = argument_stack.pop();
//            if (!lparen.equals("(")) throw new Exception("Unexpected error in message send stack");
//        }
//
//        Iterator<String> arguments_it = argument_list.iterator();
//        Iterator<String> parameters_it = parameter_types.iterator();
//        while (parameters_it.hasNext() && arguments_it.hasNext()) {
//            String arg_next = arguments_it.next();
//            String param_next = parameters_it.next();
//            if (!arg_next.equals(param_next) && !symbol_table.isParentType(arg_next, param_next))
//                throw new Exception("Method " + id + " is called with different arguments than declared");
//        }
//        if (parameters_it.hasNext() || arguments_it.hasNext())
//            throw new Exception("Parameter size is different from argument size");
//
//        return method_contents.getReturnType();
//        return null;
//    }
//
//    /**
//     * f0 -> Expression()
//     * f1 -> ExpressionTail()
//     */
//    public String visit(ExpressionList n, SymbolTable symbol_table) throws Exception {
////        argument_stack.push("(");
////        argument_stack.push(n.f0.accept(this, symbol_table));
////        n.f1.accept(this, symbol_table);
//        return null;
//    }
//
//
//    /**
//     * f0 -> ","
//     * f1 -> Expression()
//     */
//    public String visit(ExpressionTerm n, SymbolTable symbol_table) throws Exception {
////        argument_stack.push(n.f1.accept(this, symbol_table));
//        return null;
//    }


    /**
     * f0 -> IntegerLiteral()
     * | TrueLiteral()
     * | FalseLiteral()
     * | Identifier()
     * | ThisExpression()
     * | ArrayAllocationExpression()
     * | AllocationExpression()
     * | BracketExpression()
     */
    public String visit(PrimaryExpression n, SymbolTable symbol_table) throws Exception {

        String expression = n.f0.accept(this, symbol_table);

        if (isNumeric(expression)) {
            System.out.println(expression + " is numeric");
            return expression;
        } else {
            System.out.println(expression + " isn't numeric");
            String temp_reg = newTemp();

            //%val = load i32, i32* %ptr
            emit("\t" + temp_reg + " = load i32, i32* %" + expression);
            return temp_reg;
        }
    }


//
//        if (expression==null) throw new Exception("Primary expression is null. Current class = " + current_class + ", current_method = " + current_method);
//        else if (expression.equals("int") || expression.equals("boolean") || expression.equals("int[]") || expression.equals("boolean[]"))
//            return expression;
//        else if (expression.equals("true") || expression.equals("false"))
//            return "boolean";
//        else if (expression.equals("this"))
//            return this.current_class;
//        else if (symbol_table.containsClass(expression))
//            return expression;
//        else { //it's an identifier
//            String id_type = symbol_table.getTypeofIdentifier (expression, current_class, current_method);
//            if (id_type==null || id_type.equals(""))
//                throw new Exception("Cannot determine the type of " + expression);
//            return id_type;
//        }
//    }
//
    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public String visit(IntegerLiteral n, SymbolTable symbol_table) throws Exception {
        return n.f0.toString();
    }
//
//    /**
//     * f0 -> "true"
//     */
//    public String visit(TrueLiteral n, SymbolTable symbol_table) throws Exception {
//        //return "true";
//        return null;
//    }
//
//    /**
//     * f0 -> "false"
//     */
//    public String visit(FalseLiteral n, SymbolTable symbol_table) throws Exception {
////        return "false";
//        return null;
//    }
//
//
//    /**
//     * f0 -> "this"
//     */
//    public String visit(ThisExpression n, SymbolTable symbol_table) throws Exception {
//        return "this";
//        return null;
//    }
//
//
//    /**
//     * f0 -> "new"
//     * f1 -> "boolean"
//     * f2 -> "["
//     * f3 -> Expression()
//     * f4 -> "]"
//     */
//    public String visit(BooleanArrayAllocationExpression n, SymbolTable symbol_table) throws Exception {
//        String expr = n.f3.accept(this, symbol_table);
//        if (!expr.equals("int")) throw new Exception("Array intex type is " + expr + ", not int");
//        return "boolean[]";
//        return null;
//    }
//
//    /**
//     * f0 -> "new"
//     * f1 -> "int"
//     * f2 -> "["
//     * f3 -> Expression()
//     * f4 -> "]"
//     */
//    public String visit(IntegerArrayAllocationExpression n, SymbolTable symbol_table) throws Exception {
//        String expr = n.f3.accept(this, symbol_table);
//        if (!expr.equals("int")) throw new Exception("Array intex type is " + expr + ", not int");
//        return "int[]";
//        return null;
//    }
//
//
//    /**
//     * f0 -> "new"
//     * f1 -> Identifier()
//     * f2 -> "("
//     * f3 -> ")"
//     */
//    public String visit(AllocationExpression n, SymbolTable symbol_table) throws Exception {
//        String allocation_class = n.f1.accept(this, symbol_table);
//
//        if (!symbol_table.containsClass(allocation_class)) throw new Exception("Couldn't find allocation class " + allocation_class);
//        return allocation_class;
//        return null;
//    }

//    /**
//     * f0 -> "("
//     * f1 -> Expression()
//     * f2 -> ")"
//     */
//    public String visit(BracketExpression n, SymbolTable symbol_table) throws Exception {
//        return n.f1.accept(this, symbol_table);
//        return null;
//    }

}