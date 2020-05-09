package visitors;

import syntaxtree.*;
import visitor.GJDepthFirst;
import java.io.*;
import java.lang.String;
import types.*;
public class LoweringVisitor extends GJDepthFirst<String, SymbolTable> {
    private final PrintWriter pw;

    public LoweringVisitor(String current_input) throws Exception {
        String path = current_input.substring(0, current_input.length()-5);
        File file = new File(path + ".ll");
        pw = new PrintWriter(file);
    }

    void emit (String buf_output) {
        pw.println(buf_output);
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

        emit ("@." + main_class_name + "_vtable = global [0 x i8*] []\n");

        emit ("declare i8* @calloc(i32, i32)\n" +
                "declare i32 @printf(i8*, ...)\n" +
                "declare void @exit(i32)\n" +
                "\n" +
                "@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n" +
                "@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n" +
                "define void @print_int(i32 %i) {\n" +
                "    %_str = bitcast [4 x i8]* @_cint to i8*\n" +
                "    call i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n" +
                "    ret void\n" +
                "}\n" +
                "\n" +
                "define void @throw_oob() {\n" +
                "    %_str = bitcast [15 x i8]* @_cOOB to i8*\n" +
                "    call i32 (i8*, ...) @printf(i8* %_str)\n" +
                "    call void @exit(i32 1)\n" +
                "    ret void\n" +
                "}\n");

        emit("define i32 @main() {");

        n.f11.accept(this, symbol_table);
        n.f14.accept(this, symbol_table);
        n.f15.accept(this, symbol_table);

        emit("}");
        return null;
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Identifier n, SymbolTable symbol_table) throws Exception {
        return n.f0.toString();
    }

}