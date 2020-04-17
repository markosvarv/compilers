import syntaxtree.*;
import visitor.GJDepthFirst;

public class TypeCheckingVisitor extends GJDepthFirst<String, SymbolTable>{
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

        current_class = main_class_name;
        current_method = "main";
        class_var = false;

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

        current_class = classname;
        // class_var = true;
        // n.f3.accept(this, symbol_table);
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

        current_class = classname;
        // class_var = true;
        //
        // n.f5.accept(this, symbol_table);
        n.f6.accept(this, symbol_table);
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
        //String type = n.f1.accept(this, symbol_table);
        String method_name = n.f2.accept(this, symbol_table);
        current_method = method_name;

        n.f8.accept(this, symbol_table);

        String table_return_type = symbol_table.getReturnType(current_class, current_method);
        String actual_return_type = n.f10.accept(this, symbol_table);

        if (actual_return_type==null) return null;

        if (!actual_return_type.equals(table_return_type)) {
            System.err.println ("method " + method_name + " returns " + actual_return_type + ", while method is declared to return " + table_return_type);
            System.exit(1);
        }

        class_var = false;

        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public String visit(AssignmentStatement n, SymbolTable symbol_table) {
        String identifier = n.f0.accept(this, symbol_table);
        String id_type = symbol_table.getTypeofIdentifier(identifier, current_class, current_method);
        if (id_type.equals("")) {
            System.err.println("Cannot determine the type of identifier " + identifier);
            System.exit(1);
        }

        String expr_type = n.f2.accept(this, symbol_table);

        if (expr_type == null) return null;

        //if the identifier has the same type with the exrpession, then everything ok
        if (expr_type.equals(id_type)) return null;

        boolean valid_assignment = false;
        if (!id_type.equals("int") && !id_type.equals("int[]") && !id_type.equals("boolean") &&
                !expr_type.equals("int") && !expr_type.equals("int[]") && !expr_type.equals("boolean")) {
            if (!symbol_table.isParentType(expr_type, id_type)) {
                System.err.println ("Cannot assign " + expr_type + " to " + id_type + ". Assignment is not valid.");
                System.exit(1);
            }
        }
        return null;
    }

    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    public String visit(IfStatement n, SymbolTable symbol_table) {
        String expression = n.f2.accept(this, symbol_table);

        if (expression == null) return null;

        if (!expression.equals("boolean")) {
            System.err.println("expected boolean type for if expression");
            System.exit(1);
        }

        n.f4.accept(this, symbol_table);
        n.f6.accept(this, symbol_table);
        return null;
    }

    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public String visit(WhileStatement n, SymbolTable symbol_table) {
        String expression = n.f2.accept(this, symbol_table);

        if (expression == null) return null;

        if (!expression.equals("boolean")) {
            System.err.println("expected boolean type for while expression");
            System.exit(1);
        }
        n.f4.accept(this, symbol_table);
        return null;
    }

    /**
     * Grammar production:
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public String visit(AndExpression n, SymbolTable symbol_table) {
        String clause1 = n.f0.accept(this, symbol_table);
        String clause2 = n.f2.accept(this, symbol_table);
        if (!clause1.equals("boolean") || !clause2.equals("boolean")) {
            System.err.println("expected boolean types for logical and operator");
            System.exit(1);
        }
        return "boolean";
    }


    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    public String visit(NotExpression n, SymbolTable symbol_table) {
        String clause = n.f1.accept(this, symbol_table);
        if (!clause.equals("boolean")) {
            System.err.println("expected boolean type for logical not operator");
            System.exit(1);
        }
        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public String visit(CompareExpression n, SymbolTable symbol_table) {
        String pr_expr1 = n.f0.accept(this, symbol_table);
        String pr_expr2 = n.f2.accept(this, symbol_table);

        if (pr_expr1 == null || pr_expr2 == null) return null;

        if (!pr_expr1.equals("int") || !pr_expr2.equals("int")) {
            System.err.println("Cannot apply < operator between " + pr_expr1 + " and " + pr_expr2);
            System.exit(1);
        }

        return "boolean";
    }


    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public String visit(PlusExpression n, SymbolTable symbol_table) {
        String pr_expr1 = n.f0.accept(this, symbol_table);
        String pr_expr2 = n.f2.accept(this, symbol_table);

        if (pr_expr1 == null || pr_expr2 == null) return null;

        if (!pr_expr1.equals("int") || !pr_expr2.equals("int")) {
            System.err.println("Cannot apply + operator between " + pr_expr1 + " and " + pr_expr2);
            System.exit(1);
        }
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public String visit(MinusExpression n, SymbolTable symbol_table) {
        String pr_expr1 = n.f0.accept(this, symbol_table);
        String pr_expr2 = n.f2.accept(this, symbol_table);
        if (!pr_expr1.equals("int") || !pr_expr2.equals("int")) {
            System.err.println("Cannot apply - operator between " + pr_expr1 + " and " + pr_expr2);
            System.exit(1);
        }
        return "int";
    }


    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public String visit(TimesExpression n, SymbolTable symbol_table) {
        String pr_expr1 = n.f0.accept(this, symbol_table);

        String pr_expr2 = n.f2.accept(this, symbol_table);
        if (!pr_expr1.equals("int") || !pr_expr2.equals("int")) {
            System.err.println("Cannot apply * operator between " + pr_expr1 + " and " + pr_expr2);
            System.exit(1);
        }

        return "int";
    }


    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public String visit(ArrayLookup n, SymbolTable symbol_table) {
        String pr_expr_type = n.f0.accept(this, symbol_table);
        if (!pr_expr_type.equals("int[]")) {
            System.err.println("Cannot apply array lookup to " + pr_expr_type);
            System.exit(1);
        }
        pr_expr_type = n.f2.accept(this, symbol_table);
        if (!pr_expr_type.equals("int")) {
            System.err.println("Array intex type is " + pr_expr_type + ", not int");
            System.exit(1);
        }
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public String visit(ArrayLength n, SymbolTable symbol_table) {
        String pr_expr_type = n.f0.accept(this, symbol_table);
        if (!pr_expr_type.equals("int[]")) {
            System.err.println("Cannot apply .length to " + pr_expr_type);
            System.exit(1);
        }
        return "int";
    }


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
    public String visit(PrimaryExpression n, SymbolTable symbol_table) {
        String expression = n.f0.accept(this, symbol_table);

        //System.out.println("primary expression = " + expression);

        if (expression==null) {
            //System.out.println("Current class = " + current_class + " current_method = " + current_method);
            return null;
        }
        if (expression.equals("int")) {
            return "int";
        } else if (expression.equals("true") || expression.equals("false")) {
            return "boolean";
        } else if (expression.equals("this")) {
            return this.current_class;
        } else {
            String id_type = symbol_table.getTypeofIdentifier (expression, current_class, current_method);
            if (id_type==null) return "undefined";
            else return id_type;
        }
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public String visit(IntegerLiteral n, SymbolTable symbol_table) {
        return "int";
    }

    /**
     * f0 -> "true"
     */
    public String visit(TrueLiteral n, SymbolTable symbol_table) {
        return "true";
    }

    /**
     * f0 -> "false"
     */
    public String visit(FalseLiteral n, SymbolTable symbol_table) {
        return "false";
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Identifier n, SymbolTable symbol_table) {
        return n.f0.toString();
    }

    /**
     * f0 -> "this"
     */
    public String visit(ThisExpression n, SymbolTable symbol_table) {
        return "this";
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
//    public String visit(ArrayAllocationExpression n, SymbolTable symbol_table) {
        //String expr = n.f3.accept(this, symbol_table);
//        if (!expr.equals("int")) {
//            System.err.println("expected integer type for array allocation");
//            System.exit(1);
//        }
//
//        return "int[]";
//    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public String visit(AllocationExpression n, SymbolTable symbol_table) {
        String allocation_class = n.f1.accept(this, symbol_table);

        if (symbol_table.containsClass(allocation_class)) return null;
        else {
            System.out.println("Couldn't find allocation class " + allocation_class);
            System.exit(1);
        }

        return null;
    }

}
