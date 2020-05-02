import java.util.*;
import syntaxtree.*;
import visitor.GJDepthFirst;

public class TypeCheckingVisitor extends GJDepthFirst<String, SymbolTable>{
    private String current_class;
    private String current_method;
    private Stack<String> argument_stack;

    //private boolean class_var;


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
        argument_stack = new Stack<String>();

        //class_var = false;

        n.f11.accept(this, symbol_table);
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
    public String visit(ClassDeclaration n, SymbolTable symbol_table) throws Exception {
        String classname = n.f1.accept(this, symbol_table);

        current_class = classname;
        // class_var = true;
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
    public String visit(ClassExtendsDeclaration n, SymbolTable symbol_table) throws Exception {
        String classname = n.f1.accept(this, symbol_table);
        String parentclass = n.f3.accept(this, symbol_table);

        current_class = classname;
        // class_var = true;

        n.f5.accept(this, symbol_table);
        n.f6.accept(this, symbol_table);
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public String visit(VarDeclaration n, SymbolTable symbol_table) throws Exception {
        String type = n.f0.accept(this, symbol_table);
        String var = n.f1.accept(this, symbol_table);
        if (!type.equals("int") && !type.equals("boolean") && !type.equals("int[]") && !type.equals("boolean[]") && !symbol_table.containsClass(type))
            throw new Exception("Unknown type " + type + " of var " + var);
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
    public String visit(MethodDeclaration n, SymbolTable symbol_table) throws Exception {
        String declaration_return_type = n.f1.accept(this, symbol_table);
        String method_name = n.f2.accept(this, symbol_table);
        current_method = method_name;

        n.f4.accept(this, symbol_table);

        n.f7.accept(this, symbol_table);
        n.f8.accept(this, symbol_table);

        String actual_return_type = n.f10.accept(this, symbol_table);
        if (!actual_return_type.equals(declaration_return_type))
            throw new Exception("Method " + method_name + " returns " + actual_return_type + ", while is declared to return " + declaration_return_type);
        //class_var = false;
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public String visit(FormalParameter n, SymbolTable symbol_table) throws Exception {
        String type = n.f0.accept(this, symbol_table);
        String param = n.f1.accept(this, symbol_table);

        if (!type.equals("int") && !type.equals("boolean") && !type.equals("int[]") && !type.equals("boolean[]") && !symbol_table.containsClass(type))
            throw new Exception("Unknown type " + type + " of var " + param);

        return null;
    }

    /**
     * f0 -> "boolean"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(BooleanArrayType n, SymbolTable symbol_table) throws Exception {
        return "boolean[]";
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(ArrayType n, SymbolTable symbol_table) throws Exception {
        return "int[]";
    }

    /**
     * f0 -> "boolean"
     */
    public String visit(BooleanType n, SymbolTable symbol_table) throws Exception {
        return "boolean";
    }

    /**
     * f0 -> "int"
     */
    public String visit(IntegerType n, SymbolTable symbol_table) throws Exception {
        return "int";
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
        String identifier = n.f0.accept(this, symbol_table);
        String id_type = symbol_table.getTypeofIdentifier(identifier, current_class, current_method);
        if (id_type.equals("") || id_type == null)
            throw new Exception("Cannot determine the type of identifier " + identifier);

        String expr_type = n.f2.accept(this, symbol_table);
        if (expr_type == null)
            throw new Exception("Expression type in assignment is null");

        //if the identifier has the same type with the exrpession, then everything ok
        if (expr_type.equals(id_type)) return null;

        if (!id_type.equals("int") && !id_type.equals("int[]") && !id_type.equals("boolean") && !id_type.equals("boolean[]") &&
                !expr_type.equals("int") && !expr_type.equals("int[]") && !expr_type.equals("boolean") && !expr_type.equals("boolean[]")) {
            if (!symbol_table.isParentType(expr_type, id_type))
                throw new Exception("Cannot assign " + expr_type + " to " + id_type + ". Assignment is not valid.");
        }
        else throw new Exception("Cannot assign " + expr_type + " to " + id_type + ". Assignment is not valid.");
        return null;
    }


    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    public String visit(ArrayAssignmentStatement n, SymbolTable symbol_table) throws Exception {
        String expr_type = n.f2.accept(this, symbol_table);
        if (!expr_type.equals("int")) throw new Exception("Array intex type is " + expr_type + ", not int");

        String id = n.f0.accept(this, symbol_table);

        expr_type = n.f5.accept(this, symbol_table);
        if (expr_type == null) throw new Exception("Expression type in assignment is null");

        String id_type = symbol_table.getTypeofIdentifier(id, current_class, current_method);
        if (id_type.equals("") || id_type == null)
            throw new Exception("Cannot determine the type of identifier " + id);
        else if (id_type.equals("int[]")) {
            if (!expr_type.equals("int"))
                throw new Exception("Cannot assign " + expr_type + " to " + id_type + ". Assignment is not valid.");
        }else if (id_type.equals("boolean[]")) {
            if (!expr_type.equals("boolean"))
                throw new Exception("Cannot assign " + expr_type + " to " + id_type + ". Assignment is not valid.");
        }else throw new Exception("Cannot assign " + expr_type + " to " + id_type + ". Assignment is not valid.");
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
    public String visit(IfStatement n, SymbolTable symbol_table) throws Exception {
        String expression = n.f2.accept(this, symbol_table);

        if (!expression.equals("boolean")) throw new Exception("Expected boolean type for if expression, but got " + expression);

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
    public String visit(WhileStatement n, SymbolTable symbol_table) throws Exception {
        String expression = n.f2.accept(this, symbol_table);

        if (!expression.equals("boolean")) throw new Exception("Expected boolean type for while expression, but got " + expression);
        n.f4.accept(this, symbol_table);
        return null;
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public String visit(PrintStatement n, SymbolTable symbol_table) throws Exception {
        String expr = n.f2.accept(this, symbol_table);
        if (!expr.equals("int") && !expr.equals("boolean"))
            throw new Exception("Print statement expected int or boolean, but got " + expr);
        return null;
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public String visit(AndExpression n, SymbolTable symbol_table) throws Exception {
        String clause1 = n.f0.accept(this, symbol_table);
        String clause2 = n.f2.accept(this, symbol_table);

        if (!clause1.equals("boolean") || !clause2.equals("boolean"))
            throw new Exception("Cannot apply && operator between " + clause1 + " and " + clause2);
        return "boolean";
    }


    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    public String visit(NotExpression n, SymbolTable symbol_table) throws Exception {
        String clause = n.f1.accept(this, symbol_table);
        if (!clause.equals("boolean")) throw new Exception("Expected boolean type for logical not operator, but got " + clause);
        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public String visit(CompareExpression n, SymbolTable symbol_table) throws Exception {
        String pr_expr1 = n.f0.accept(this, symbol_table);
        String pr_expr2 = n.f2.accept(this, symbol_table);

        if (!pr_expr1.equals("int") || !pr_expr2.equals("int"))
            throw new Exception("Cannot apply < operator between " + pr_expr1 + " and " + pr_expr2);
        return "boolean";
    }


    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public String visit(PlusExpression n, SymbolTable symbol_table) throws Exception {
        String pr_expr1 = n.f0.accept(this, symbol_table);
        String pr_expr2 = n.f2.accept(this, symbol_table);

        if (!pr_expr1.equals("int") || !pr_expr2.equals("int"))
            throw new Exception("Cannot apply + operator between " + pr_expr1 + " and " + pr_expr2);
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public String visit(MinusExpression n, SymbolTable symbol_table) throws Exception {
        String pr_expr1 = n.f0.accept(this, symbol_table);
        String pr_expr2 = n.f2.accept(this, symbol_table);
        if (!pr_expr1.equals("int") || !pr_expr2.equals("int"))
            throw new Exception("Cannot apply - operator between " + pr_expr1 + " and " + pr_expr2);
        return "int";
    }


    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public String visit(TimesExpression n, SymbolTable symbol_table) throws Exception {
        String pr_expr1 = n.f0.accept(this, symbol_table);

        String pr_expr2 = n.f2.accept(this, symbol_table);
        if (!pr_expr1.equals("int") || !pr_expr2.equals("int"))
            throw new Exception("Cannot apply * operator between " + pr_expr1 + " and " + pr_expr2);

        return "int";
    }


    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public String visit(ArrayLookup n, SymbolTable symbol_table) throws Exception {
        String pr_expr_type = n.f2.accept(this, symbol_table);
        if (!pr_expr_type.equals("int")) throw new Exception("Array intex type is " + pr_expr_type + ", not int");

        pr_expr_type = n.f0.accept(this, symbol_table);
        if (pr_expr_type.equals("int[]")) return "int";
        else if (pr_expr_type.equals("boolean[]")) return "boolean";
        else throw new Exception("Cannot apply array lookup to " + pr_expr_type);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public String visit(ArrayLength n, SymbolTable symbol_table) throws Exception {
        String pr_expr_type = n.f0.accept(this, symbol_table);
        if (!pr_expr_type.equals("int[]") && !pr_expr_type.equals("boolean[]"))
            throw new Exception("Cannot apply .length to " + pr_expr_type);
        return "int";
    }


    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public String visit(MessageSend n, SymbolTable symbol_table) throws Exception {
        String pr_expr = n.f0.accept(this, symbol_table);
        String id = n.f2.accept(this, symbol_table);

        //System.out.println("pr_expr = " + pr_expr + " id = " + id);
        MethodContents method_contents = symbol_table.getMethodContents (pr_expr, id);
        if (method_contents==null) throw new Exception("Cannot get method " + id + " contents");
        LinkedList<String> parameter_types = method_contents.getParameterTypes();

        n.f4.accept(this, symbol_table);
        LinkedList<String> argument_list = new LinkedList<String>();

        //System.out.println("argument stack = " + argument_stack);

        if (!argument_stack.empty() && !argument_stack.peek().equals("(")) {
            do argument_list.addFirst(argument_stack.pop());
            while (!argument_stack.peek().equals("("));
            String lparen = argument_stack.pop();
            if (!lparen.equals("(")) throw new Exception("Unexpected error in message send stack");
        }
        //System.out.println("parameters list = " + symbol_table.getParameterTypes(pr_expr, id));
        //System.out.println("arguments list = " + argument_list);

        //System.out.println("current class = " + current_class + " current method = " + current_method);

        Iterator<String> arguments_it = argument_list.iterator();
        Iterator<String> parameters_it = parameter_types.iterator();
        while (parameters_it.hasNext() && arguments_it.hasNext()) {
            String arg_next = arguments_it.next();
            String param_next = parameters_it.next();
            if (!arg_next.equals(param_next) && !symbol_table.isParentType(arg_next, param_next))
                throw new Exception("Method " + id + " is called with different arguments than declared");
        }
        if (parameters_it.hasNext() || arguments_it.hasNext())
            throw new Exception("Parameter size is different from argument size");

        return method_contents.getReturnType();
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    public String visit(ExpressionList n, SymbolTable symbol_table) throws Exception {
        argument_stack.push("(");
        argument_stack.push(n.f0.accept(this, symbol_table));
        n.f1.accept(this, symbol_table);
        return null;
    }


    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    public String visit(ExpressionTerm n, SymbolTable symbol_table) throws Exception {
        argument_stack.push(n.f1.accept(this, symbol_table));
        return null;
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
    public String visit(PrimaryExpression n, SymbolTable symbol_table) throws Exception {
        String expression = n.f0.accept(this, symbol_table);

        if (expression==null) throw new Exception("Primary expression is null. Current class = " + current_class + ", current_method = " + current_method);
        else if (expression.equals("int") || expression.equals("boolean") || expression.equals("int[]") || expression.equals("boolean[]"))
            return expression;
        else if (expression.equals("true") || expression.equals("false"))
            return "boolean";
        else if (expression.equals("this"))
            return this.current_class;
        else if (symbol_table.containsClass(expression))
            return expression;
        else { //it's an identifier
            String id_type = symbol_table.getTypeofIdentifier (expression, current_class, current_method);
            if (id_type==null || id_type.equals(""))
                throw new Exception("Cannot determine the type of " + expression);
            return id_type;
        }
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public String visit(IntegerLiteral n, SymbolTable symbol_table) throws Exception {
        return "int";
    }

    /**
     * f0 -> "true"
     */
    public String visit(TrueLiteral n, SymbolTable symbol_table) throws Exception {
        return "true";
    }

    /**
     * f0 -> "false"
     */
    public String visit(FalseLiteral n, SymbolTable symbol_table) throws Exception {
        return "false";
    }


    /**
     * f0 -> "this"
     */
    public String visit(ThisExpression n, SymbolTable symbol_table) throws Exception {
        return "this";
    }


    /**
     * f0 -> "new"
     * f1 -> "boolean"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public String visit(BooleanArrayAllocationExpression n, SymbolTable symbol_table) throws Exception {
        String expr = n.f3.accept(this, symbol_table);
        if (!expr.equals("int")) throw new Exception("Array intex type is " + expr + ", not int");
        return "boolean[]";
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public String visit(IntegerArrayAllocationExpression n, SymbolTable symbol_table) throws Exception {
        String expr = n.f3.accept(this, symbol_table);
        if (!expr.equals("int")) throw new Exception("Array intex type is " + expr + ", not int");
        return "int[]";
    }


    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public String visit(AllocationExpression n, SymbolTable symbol_table) throws Exception {
        String allocation_class = n.f1.accept(this, symbol_table);

        if (!symbol_table.containsClass(allocation_class)) throw new Exception("Couldn't find allocation class " + allocation_class);
        return allocation_class;
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public String visit(BracketExpression n, SymbolTable symbol_table) throws Exception {
        return n.f1.accept(this, symbol_table);
    }
}
