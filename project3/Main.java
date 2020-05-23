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

            try{
                fis = new FileInputStream(current_input);
                MiniJavaParser parser = new MiniJavaParser(fis);
                FillTableVisitor eval = new FillTableVisitor();
                TypeCheckingVisitor type_ch = new TypeCheckingVisitor();
                LoweringVisitor ll_visitor = new LoweringVisitor(current_input);
                Goal root = parser.Goal();
                System.out.println("Program parsed successfully.");
                try {
                    root.accept(eval, symbol_table);
                    root.accept(type_ch, symbol_table);
                    symbol_table.printOffsets();
                    root.accept(ll_visitor, symbol_table);
                }catch (Exception exc) {
                    System.err.println("Error: " + exc.getMessage());
                }
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
            System.out.println("-------------------------------");
        }
    }
}
