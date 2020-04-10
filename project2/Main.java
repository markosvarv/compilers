import syntaxtree.*;
import visitor.*;
import java.io.*;

class Main {
    public static void main (String [] args){
    	if(args.length == 0){
    	    System.err.println("Usage: java [MainClassName] [file1] [file2] ... [fileN]");
    	    System.exit(1);
    	}
    	FileInputStream fis = null;

        for (String current_input : args) {
            SymbolTable symbol_table = new SymbolTable();

        	try{
        	    fis = new FileInputStream(current_input);
        	    MiniJavaParser parser = new MiniJavaParser(fis);
        	    System.err.println("Program parsed successfully.");
        	    FillTableVisitor eval = new FillTableVisitor();
                TypeCheckingVisitor type_ch = new TypeCheckingVisitor();
        	    Goal root = parser.Goal();
        	    root.accept(eval, symbol_table);
                root.accept(type_ch, symbol_table);
                symbol_table.printOffsets();
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
        }
    }
}
