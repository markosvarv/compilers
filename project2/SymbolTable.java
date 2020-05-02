import java.util.*;

public class SymbolTable {
    private LinkedHashMap <String, ClassContents> symbol_table;

    public SymbolTable () {
        symbol_table = new LinkedHashMap <String, ClassContents>();
    }

    //add a field in the class
    public boolean addVar (String class_name, String type, String member_name) {
        ClassContents contents = symbol_table.get(class_name);
        if (contents==null) {
            System.err.println("Class " + class_name + " already exists!");
            return false;
        }
        if (!contents.VarAdd (type, member_name)) {
            System.err.println ("Variable " + member_name + " has already been declared in class " + class_name);
            return false;
        }
        return true;
    }

    //add a variable in a function in the class
    public boolean addVar (String class_name, String method_name, String type, String member_name) {
        ClassContents contents = symbol_table.get(class_name);
        if (contents==null) {
            System.err.println("Class " + class_name + " already exists!");
            return false;
        }
        return contents.VarAdd (method_name, type, member_name);
    }

    public boolean newClass (String class_name, String parent_class, boolean is_main) {
        if (symbol_table.putIfAbsent (class_name, new ClassContents (class_name, parent_class, is_main)) == null) return true;
        else return false;
    }

    public boolean addMethod (String class_name, String method_name, String type) {
        ClassContents content = symbol_table.get(class_name);
        if (content == null) {
            System.err.println("Cannot find class " + class_name + " in symbol table.");
            return false;
        }
        if (!content.newMethod (method_name, type)) {
            System.err.println("Method " + method_name + " already exists.");
            return false;
        }
        return true;
    }

    //add parameters in a method
    public boolean addParameters (String class_name, String method_name, String param_type, String param_name) {
        ClassContents content = symbol_table.get(class_name);
        if (content == null) {
            System.err.println("Cannot find class " + class_name + " in symbol table");
            return false;
        }
        return content.addParameters (method_name, param_type, param_name);
    }

    public boolean overrideCheck (String class_name, String method_name, String return_type) {
        ClassContents child_class_contents = symbol_table.get(class_name);
        if (child_class_contents == null) {
            System.err.println("Cannot find class " + class_name + " in symbol table");
            return false;
        }

        MethodContents child_method_contents = child_class_contents.methods.get(method_name);
        if (child_method_contents == null) {
            System.err.println("Cannot find method " + method_name + " in class " + class_name);
            return false;
        }
        ClassContents current_class_contents = child_class_contents;

        //for every parent class make an override checkgfg
        while (current_class_contents.getParentClass()!=null) {
            current_class_contents = symbol_table.get(current_class_contents.parent_class);
            MethodContents current_method_contents = current_class_contents.methods.get(method_name);

            //if function doesn't exist in parent class, then no override problem
            if (current_method_contents==null) continue;

            //if function exists, then must have the same return type and parameter types
            if (!return_type.equals(current_method_contents.getReturnType())) {
                System.err.println ("Overriding methods must have the same return type");
                return false;
            }

            LinkedList<String> current_contents_list = new LinkedList<String>(current_method_contents.parameters.values());
            LinkedList<String> child_contents_list = new LinkedList<String>(child_method_contents.parameters.values());

            if (!current_contents_list.equals(child_contents_list)) {
                System.err.println("Overriding method " + current_method_contents.method_name + " must have the same type and number of parameters in classes " + current_class_contents.class_name + " and " + child_class_contents.class_name);
                return false;
            }
            child_method_contents.override_method = true;
        }
        return true;
    }


    public void printOffsets () {
        for (Map.Entry<String, ClassContents> class_entry : symbol_table.entrySet()) {
            String class_key = class_entry.getKey();
            ClassContents current_class_contents = class_entry.getValue();
            if (!current_class_contents.is_main) System.out.println("\n-----Class: " + current_class_contents.class_name + "-----");

            if (current_class_contents.parent_class==null) {
                current_class_contents.fields_offset_sum = 0;
                current_class_contents.methods_offset_sum = 0;
            }
            else {
                ClassContents parent_class_contents = symbol_table.get(current_class_contents.parent_class);
                if (parent_class_contents == null) {
                    System.out.println("Cannot find parent class");
                    return;
                }
                current_class_contents.fields_offset_sum = parent_class_contents.fields_offset_sum;
                current_class_contents.methods_offset_sum = parent_class_contents.methods_offset_sum;
            }
            if (!current_class_contents.is_main) System.out.println("--Variables--");
            for (Map.Entry<String, String> field_entry : current_class_contents.fields.entrySet()) {
                String field_key = field_entry.getKey();
                String current_field_type = field_entry.getValue();
                int current_offset = -1;
                switch(current_field_type) {
                    case "int":
                        current_offset = 4;
                        break;
                    case "boolean":
                        current_offset = 1;
                        break;
                    case "int[]":
                        current_offset = 8;
                        break;
                    case "boolean[]":
                        current_offset = 8;
                        break;
                    default:
                        current_offset = 8;
                }
                System.out.println(class_key + '.' + field_key + " : " + current_class_contents.fields_offset_sum);
                current_class_contents.fields_offset_sum += current_offset;
            }
            if (!current_class_contents.is_main) System.out.println("---Methods---");
            for (Map.Entry<String, MethodContents> method_entry : current_class_contents.methods.entrySet()) {
                String method_key = method_entry.getKey();
                MethodContents current_method_contents = method_entry.getValue();
                if (current_class_contents.is_main) continue; //for Main class
                if (!current_method_contents.override_method) {
                    System.out.println(class_key + '.' + method_key + " : " + current_class_contents.methods_offset_sum);
                    current_class_contents.methods_offset_sum += 8;
                }
            }
        }
    }

    public String getTypeofIdentifier (String id, String class_name, String method_name) {
        ClassContents current_class_contents = symbol_table.get(class_name);
        if (current_class_contents == null) {
            System.err.println("Cannot find class " + class_name + " in symbol table");
            return null;
        }

        MethodContents method_contents = current_class_contents.methods.get(method_name);
        if (method_contents == null) {
            System.err.println("Cannot find method " + method_name + " in class " + class_name);
            return null;
        }

        if (method_contents.parameters.containsKey(id)) return method_contents.parameters.get(id);
        if (method_contents.variables.containsKey(id)) return method_contents.variables.get(id);

        if (current_class_contents.fields.containsKey(id)) return current_class_contents.fields.get(id);

        //search for the variable in parent classes
        while (current_class_contents.parent_class != null){
            current_class_contents = symbol_table.get(current_class_contents.parent_class);
            if (current_class_contents.fields.containsKey(id)) return current_class_contents.fields.get(id);
        }
        return "";
    }

    public MethodContents getMethodContents (String class_name, String method_name){
        ClassContents class_contents = symbol_table.get(class_name);
        if (class_contents == null) {
            System.err.println("Cannot find class " + class_name + " in symbol table");
            return null;
        }

        MethodContents method_contents = class_contents.methods.get(method_name);

        if (method_contents != null) return method_contents;

        while (class_contents.parent_class!=null) {
            class_contents = symbol_table.get(class_contents.parent_class);
            method_contents = class_contents.methods.get(method_name);
            if (method_contents != null) return method_contents;
        }

        System.err.println("Cannot find method " + method_name + " in class " + class_name + " and parent classes");
        return null;
    }

    public boolean isParentType (String class_name, String type) {
        ClassContents class_contents = symbol_table.get(class_name);
        if (class_contents == null) {
            System.err.println("Cannot find class " + class_name + " in symbol table");
            return false;
        }

        while (class_contents.parent_class != null) {
            if (class_contents.parent_class.equals(type)) return true;
            class_contents = symbol_table.get(class_contents.parent_class);
        }
        return false;
    }

    public boolean containsClass (String class_name) {
        return symbol_table.containsKey(class_name);
    }
}

class ClassContents {
    LinkedHashMap <String, String> fields;
    LinkedHashMap <String, MethodContents> methods;
    String class_name;
    String parent_class;
    boolean is_main;
    int fields_offset_sum;
    int methods_offset_sum;

    public ClassContents (String given_class_name, String given_parent_class, boolean main_class) {
        fields = new LinkedHashMap<String, String>();
        methods = new LinkedHashMap<String, MethodContents>();
        class_name = given_class_name;
        parent_class = given_parent_class;
        is_main = main_class;
    }

    public String getParentClass () {
        return parent_class;
    }

    public boolean VarAdd (String type, String member_name) {
        if (fields.putIfAbsent (member_name, type) == null) return true;
        else return false;
    }

    public boolean VarAdd (String method_name, String type, String member_name) {
        MethodContents method_contents = methods.get(method_name);
        if (method_contents==null) {
            System.err.println("Method " + method_name + " isn't declared.");
            return false;
        }
        return method_contents.variablesAdd (type, member_name);
    }

    public boolean newMethod (String method_name, String return_type) {
        if (methods.putIfAbsent (method_name, new MethodContents (method_name, return_type)) == null) return true;
        else return false;
    }

    public boolean addParameters (String method_name, String param_type, String param_name) {
        MethodContents method_contents = methods.get(method_name);

        if (method_contents==null) {
            System.err.println("Method " + method_name + " isn't declared.");
            return false;
        }
        if (method_contents.parametersAdd (param_type, param_name)) return true;
        else {
            System.err.println("Parameter " + param_name + " already exists in method " + method_name);
            return false;
        }
    }
}

class MethodContents {
    String method_name;
    String return_type;
    Boolean override_method;

    LinkedHashMap <String, String> parameters;
    LinkedHashMap <String, String> variables;

    public MethodContents (String given_method_name, String given_return_type) {
        parameters = new LinkedHashMap<String, String>();
        variables = new LinkedHashMap<String, String>();
        method_name = given_method_name;
        return_type = given_return_type;
        override_method = false;
    }

    public boolean parametersAdd (String type, String name) {
        if (parameters.putIfAbsent (name, type) == null) return true;
        else return false;
    }

    public boolean variablesAdd (String type, String name) {
        if (parameters.containsKey(name)) {
            System.err.println("Variable " + name + " is already declared as a parameter in method " + method_name);
            return false;
        }

        if (variables.putIfAbsent (name, type) == null) return true;
        else {
            System.err.println ("Variable " + name + " is already declared in method " + method_name);
            return false;
        }
    }

    public String getReturnType () {
        return return_type;
    }

    public LinkedList<String> getParameterTypes() {
        return new LinkedList<String>(parameters.values());
    }
}
