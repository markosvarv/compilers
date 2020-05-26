package visitors;

import syntaxtree.*;
import visitor.GJDepthFirst;
import java.io.*;
import java.lang.String;
import java.util.*;

import types.*;

public class LoweringVisitor extends GJDepthFirst<String, SymbolTable> {
    private String current_class;
    private String current_method;
    private final PrintWriter pw;
    private int current_reg_num;
    private int current_if_label_num;
    private String pr_expr_var;
    private final HashMap<String, Integer> methods_number;
    private LinkedHashMap<String, String> parameters_map;
    private final Stack<String> argument_stack;


    public LoweringVisitor(String current_input) throws Exception {
        String path = current_input.substring(0, current_input.length()-5);
        File file = new File(path + ".ll");
        pw = new PrintWriter(file);
        current_reg_num = 0;
        methods_number = new HashMap<>();
        argument_stack = new Stack<>();
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
        pw.print(buf_output);
    }

    String getLLVMType (String java_type) {
        switch (java_type) {
            case "int":
                return "i32";
            case "boolean":
                return "i1";
            default:
                return "i8*";
                //throw new Exception ("cannot determine type " + java_type);
        }
    }

    String getMethodSignature (MethodContents current_method_contents) {
        StringBuilder buf= new StringBuilder();
        buf.append(getLLVMType(current_method_contents.getReturnType())).append(" (");
        LinkedList<String> parameter_types = current_method_contents.getParameterTypes();
        buf.append("i8*"); //*this* pointer

        for (String current_parameter_type : parameter_types) {
            buf.append(", ").append(getLLVMType(current_parameter_type));
        }
        buf.append(")*");
        return buf.toString();
    }

    String emitClassFieldCode (String llvm_type) {
        //    %_1 = getelementptr i8, i8* %this, i32 8
        String field_pointer_reg = newTemp();

        //TODO edw xreiazontai offsets
        //TODO na dw ti ginetai me tin dilwsi pediwn goneikwn klasewn
        emit('\t' + field_pointer_reg + " = getelementptr i8, i8* %this, i32 " + "8" + '\n');

        //    %_2 = bitcast i8* %_1 to i32*
        String bitcast_reg = newTemp();
        emit('\t' + bitcast_reg + " = bitcast i8* " + field_pointer_reg + " to " + llvm_type + "*\n");
        return bitcast_reg;
    }

    void declareVTables(SymbolTable symbol_table) {
        Collection<ClassContents> classes = symbol_table.getClasses();
        for (ClassContents current_class_contents : classes) {
            if (current_class_contents.getIsMain())
                emit("@." + current_class_contents.getClassName() + "_vtable = global [0 x i8*] []\n\n");
            else {
                emit("@." + current_class_contents.getClassName() + "_vtable = global [");
                StringBuilder buf= new StringBuilder();
                Set<String> method_set = new HashSet<>();
                String parent_class;
                String class_name = current_class_contents.getClassName();
                do {
                    Collection<MethodContents> methods = current_class_contents.getMethodContents();
                    for (MethodContents current_method_contents : methods) {
                        if (!method_set.add(current_method_contents.getMethodName())) continue;
                        buf.append("\ti8* bitcast(").append(getMethodSignature(current_method_contents));
                        buf.append(" @").append(current_class_contents.getClassName()).append(".").append(current_method_contents.getMethodName()).append(" to i8*),\n");
                    }
                    parent_class = current_class_contents.getParentClass();
                }while (parent_class != null && (current_class_contents = symbol_table.getClassContents(parent_class))!=null);
                //delete the last comma
                buf.deleteCharAt(buf.length()-2);
                buf.append("]\n\n");
                emit(method_set.size() + " x i8*] [\n" + buf.toString());
                methods_number.put(class_name, method_set.size());
            }
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
        current_class = n.f1.accept(this, symbol_table);
        current_method = "main";

        declareVTables(symbol_table);

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
                "}\n\n");

        emit("define i32 @main() {\n");

        n.f11.accept(this, symbol_table);
        n.f14.accept(this, symbol_table);
        n.f15.accept(this, symbol_table);

        emit("\tret i32 0\n}\n");

        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public String visit(VarDeclaration n, SymbolTable symbol_table) throws Exception {
        String java_type = n.f0.accept(this, symbol_table);
        String llvm_type = getLLVMType(java_type);
        String id = n.f1.accept(this, symbol_table);
        emit("\t%" + id + " = alloca " + llvm_type + '\n');
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
        current_class = n.f1.accept(this, symbol_table);
        //n.f3.accept(this, symbol_table);
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
        current_class = n.f1.accept(this, symbol_table);
        //n.f5.accept(this, symbol_table);
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
    public String visit(MethodDeclaration n, SymbolTable symbol_table) throws Exception {
        current_reg_num = 0;
        String declaration_return_type = n.f1.accept(this, symbol_table);
        current_method = n.f2.accept(this, symbol_table);
        parameters_map = new LinkedHashMap<>();

        emit("\ndefine " + getLLVMType(declaration_return_type) + " @" + current_class + '.' + current_method + "(i8* %this");
        n.f4.accept(this, symbol_table);
        emit (") {\n");

        for (Map.Entry<String, String> field_entry : parameters_map.entrySet()) {
            String parameter_id = field_entry.getKey();
            String parameter_llvm_type = field_entry.getValue();
            emit("\t%" + parameter_id + " = alloca " + parameter_llvm_type + '\n');
            emit("\tstore " + parameter_llvm_type + " %." + parameter_id + ", " + parameter_llvm_type + "* %" + parameter_id + '\n');
        }
        parameters_map = null;
        n.f7.accept(this, symbol_table);
        n.f8.accept(this, symbol_table);

        String return_expr = n.f10.accept(this, symbol_table);
        emit("\tret i32 " + return_expr + "\n}\n");

        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public String visit(FormalParameter n, SymbolTable symbol_table) throws Exception {
        String java_type = n.f0.accept(this, symbol_table);
        String id = n.f1.accept(this, symbol_table);

        String llvm_type = getLLVMType(java_type);
        parameters_map.put(id, llvm_type);

        emit(", " + llvm_type + " %." + id);
        return null;
    }
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
        String id = n.f0.accept(this, symbol_table);
        String java_type = symbol_table.getTypeofIdentifier(id, current_class, current_method);
        String llvm_type = getLLVMType(java_type);
        //System.out.println("llvm type of " + id + " with java type = " + java_type + " = " + llvm_type);
        String expr = n.f2.accept(this, symbol_table);
        //System.out.println ("expr = " + expr);

        if (symbol_table.isClassField(id, current_class, current_method)) {
            String field_reg = emitClassFieldCode(llvm_type);
            emit("\tstore " + llvm_type + " " + expr + ", " + llvm_type + "* " + field_reg + '\n');
        }
        else emit("\tstore " + llvm_type + " " + expr + ", " + llvm_type + "* %" + id + '\n');

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
        emit("\tbr i1 " + expression + ", label %" + if_label + ", label %" + else_label + '\n');

        emit("\n\t" + if_label + ":\n");
        n.f4.accept(this, symbol_table);
        //br label %goto
        emit("\tbr label %" + end_label + '\n');

        emit("\n\t" + else_label + ":\n");
        n.f6.accept(this, symbol_table);
        //br label %goto
        emit("\tbr label %" + end_label + '\n');

        emit("\n\t" + end_label + ":\n");
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
        emit("\tcall void (i32) @print_int(i32 " + expr + ")\n");
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
        emit("\t" + temp_reg + " = icmp slt i32 " + pr_expr1 + ", " + pr_expr2 + '\n');
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


    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public String visit(TimesExpression n, SymbolTable symbol_table) throws Exception {
        String pr_expr1 = n.f0.accept(this, symbol_table);
        String pr_expr2 = n.f2.accept(this, symbol_table);

        //%sum = mul i32 %a, %b
        String result_reg = newTemp();
        emit('\t' + result_reg + " = mul i32 " + pr_expr1 + ", " + pr_expr2 + '\n');
        return result_reg;
    }

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
    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public String visit(MessageSend n, SymbolTable symbol_table) throws Exception {
        String pr_expr_reg = n.f0.accept(this, symbol_table);
        String id = n.f2.accept(this, symbol_table);

        System.out.println("id = " + id + " current class = " + current_class + " current_method = " + current_method);
        String class_name = symbol_table.getTypeofIdentifier (pr_expr_var, current_class, current_method);
        System.out.println("message send class_name = " + class_name);
        MethodContents method_contents = symbol_table.getMethodContents(class_name, id);
        String signature = getMethodSignature(method_contents);

        //%ptr = bitcast i32* %ptr2 to i8**
        String bitcast_reg = newTemp();
        emit("\n\t" + bitcast_reg + " = bitcast i8* " + pr_expr_reg + " to i8***\n");

        //%val = load i32, i32* %ptr
        String load_reg = newTemp();
        emit('\t' + load_reg + " = load i8**, i8*** " + bitcast_reg + '\n');

        System.out.println("method " + method_contents.getMethodName() + " offset = " + method_contents.getMethodOffset());
        String element_pointer = newTemp();
        emit('\t' + element_pointer + " = getelementptr i8*, i8** " + load_reg + ", i32 " + method_contents.getMethodOffset() + '\n');

        String function_pointer = newTemp();
        emit('\t' + function_pointer + " = load i8*, i8** " + element_pointer + '\n');

        String signature_pointer = newTemp();
        emit ('\t' + signature_pointer + " = bitcast i8* " + function_pointer + " to " + signature + '\n');

        String call_reg = newTemp();
        emit('\t' + call_reg + " = call " + "i32 " + signature_pointer +  "(i8* " + pr_expr_reg);

        n.f4.accept(this, symbol_table);

        if (!argument_stack.empty() && !argument_stack.peek().equals("(")) {
            //TODO: na dw mipos ta kanw emit apeutheias sto expressionlist
            do {
                String expression = argument_stack.pop();
                if(isNumeric(expression)) {
                    emit(", i32 " + expression);
                } else {
                    emit(", " + expression);
                }

            } while (!argument_stack.peek().equals("("));
            String lparen = argument_stack.pop();
            if (!lparen.equals("(")) throw new Exception("Unexpected error in message send stack");
        }

        emit(")\n\n");


        return call_reg;


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

        if (isNumeric(expression) || expression.startsWith("%")) {
            //System.out.println("mphka sto if expr = " + expression);
            return expression;
        }
        else {
            pr_expr_var = expression;
            String java_type = symbol_table.getTypeofIdentifier(expression, current_class, current_method);
            String llvm_type = getLLVMType(java_type);
            String temp_reg;
            if (symbol_table.isClassField(expression, current_class, current_method)) {
                String field_reg = emitClassFieldCode(llvm_type);
                temp_reg = newTemp();
                emit("\t" + temp_reg + " = load " + llvm_type + ", " + llvm_type + "* " + field_reg + '\n');
            } else {
                temp_reg = newTemp();
                emit("\t" + temp_reg + " = load " + llvm_type + ", " + llvm_type + "* %" + expression + '\n');
            }

            return temp_reg;
        }
    }

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
    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public String visit(AllocationExpression n, SymbolTable symbol_table) throws Exception {
        String allocation_class = n.f1.accept(this, symbol_table);

        //%result = call i8* @calloc(i32 1, i32 %val)
        String calloc_reg = newTemp();

        ClassContents class_contents = symbol_table.getClassContents(allocation_class);

        System.out.println("class " + class_contents.getClassName() + " offset sum = " + class_contents.getFields_offset_sum());

        emit ("\n\t" + calloc_reg + " = call i8* @calloc(i32 1, i32 " + (8 + class_contents.getFields_offset_sum()) + ")\n");

        //%ptr = bitcast i32* %ptr2 to i8**
        String bitcast_reg = newTemp();
        emit ('\t' + bitcast_reg + " = bitcast i8* " + calloc_reg + " to i8***\n");

        //%ptr_idx = getelementptr i8, i8* %ptr, i32 %idx
        String getelementptr_reg = newTemp();
        int current_methods_num = methods_number.get(allocation_class);
        emit('\t' + getelementptr_reg + " = getelementptr [" + current_methods_num + " x i8*], [" + current_methods_num + " x i8*]* @." + allocation_class + "_vtable, i32 0, i32 0\n") ;

        //store i32 %val, i32* %ptr
        emit("\tstore i8** " + getelementptr_reg + ", i8*** " + bitcast_reg + "\n\n");
        return calloc_reg;
    }

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