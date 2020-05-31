import syntaxtree.*;
import visitor.*;
import java.io.*;
import types.*;
import visitors.*;

class Main {
    public static void main (String [] args) throws Exception {
        if(args.length == 0){
            System.err.println("Usage: java [MainClassName] [file1] [file2] ... [fileN]");
            System.exit(1);
        }
        FileInputStream fis = null;

        for (String current_input : args) {
            SymbolTable symbol_table = new SymbolTable();
            System.out.println("\nFile: " + current_input);
            String ll_file_path = current_input.substring(0, current_input.length()-5);
            ll_file_path += ".ll";

            try{
                fis = new FileInputStream(current_input);
                MiniJavaParser parser = new MiniJavaParser(fis);
                FillTableVisitor eval = new FillTableVisitor();
                TypeCheckingVisitor type_ch = new TypeCheckingVisitor();
                LoweringVisitor ll_visitor = new LoweringVisitor(ll_file_path);
                Goal root = parser.Goal();
                System.out.println("\tProgram parsed successfully.");
                try {
                    root.accept(eval, symbol_table);
                    root.accept(type_ch, symbol_table);
                    symbol_table.calculateOffsets();
                }catch (Exception exc) {
                    System.err.println("Error: " + exc.getMessage());
                }
                System.out.println("\tProgram type checking completed successfully.");
                root.accept(ll_visitor, symbol_table);
                System.out.println("\tProgram lowering completed successfully. Saved in " + ll_file_path);
            }
            catch(ParseException ex){
                System.out.println(ex.getMessage());
            }
            catch(FileNotFoundException ex){
                System.err.println(ex.getMessage());
            }
            finally{
                try{
                    if(fis != null) fis.close();
                }
                catch(IOException ex){
                    System.err.println(ex.getMessage());
                }
            }
            System.out.println();
        }
    }
}
