import java.util.*;

public class SymbolTable {
    private LinkedHashMap <String, ClassContents> symbol_table;

    public SymbolTable () {
        symbol_table = new LinkedHashMap <String, ClassContents>();
    }

    //add a field in the class
    public boolean addVar (String class_name, String type, String member_name) {
        ClassContents contents = symbol_table.get(class_name);
        if (contents==null) return false;
        if (!contents.VarAdd (type, member_name)) {
            System.err.println ("member " + member_name + " has already been declared");
            System.exit(1);
        }
        return true;
    }

    //add a variable in a function in the class
    public boolean addVar (String class_name, String method_name, String type, String member_name) {
        ClassContents contents = symbol_table.get(class_name);
        if (contents==null) return false;
        if (!contents.VarAdd (method_name, type, member_name)) {
            System.err.println ("member " + member_name + " has already been declared");
            System.exit(1);
        }
        return true;
    }

    public boolean containsClass (String class_name) {
        return symbol_table.containsKey(class_name);
    }

    public boolean newClass (String class_name, String parent_class) {
        if (containsClass(class_name)) return false;
        ClassContents content = new ClassContents (class_name, parent_class);
        if (symbol_table.putIfAbsent (class_name, content) == null) return true;
        else return false;
    }

    public void addMethod (String class_name, String method_name, String type) {
        ClassContents content = symbol_table.get(class_name);
        if (content == null) {
            System.err.println("cannot find class " + class_name + " in symbol table.");
            System.exit(1);
        }
        if (!content.newMethod (method_name, type)) {
            System.err.println("method " + method_name + " already exists!");
            System.exit(1);
        }
    }

    public boolean overrideCheck (String class_name, String method_name, String return_type) {
        ClassContents child_class_contents = symbol_table.get(class_name);
        if (child_class_contents == null) {
            System.err.println("Cannot find class " + class_name + " in symbol table");
            System.exit(1);
        }

        MethodContents child_method_contents = child_class_contents.methods.get(method_name);


        ClassContents current_class_contents = child_class_contents;
        //for every parent class make an override check

        //boolean override = false;

        while (current_class_contents.getParentClass()!=null) {
            current_class_contents = symbol_table.get(current_class_contents.parent_class);


            MethodContents current_method_contents = current_class_contents.methods.get(method_name);

            //if function doesn't exist in parent class, then no override problem
            if (current_method_contents==null) {
                //System.out.println(method_name + " null contents");
                continue;
            }
            //if function exists, then must have the same return type and parameters
            if (!return_type.equals(current_method_contents.getReturnType())) {
                System.err.println ("overriding methods must have the same return type");
                //System.out.println ("name = " + method_name + " type = " + return_type);
                System.exit(1);
            }

            if (current_method_contents.parameters.size() != child_method_contents.parameters.size()){
                System.err.println ("overriding methods must have the same number of parameters");
                System.exit(1);
            }

            for (Map.Entry<String, String> entry1 : current_method_contents.parameters.entrySet()) {
                String key = entry1.getKey();
                String value1 = entry1.getValue();
                String value2 = child_method_contents.parameters.get(key);

                if (!value1.equals(value2)) {
                    System.err.println("Overriding methods must have the same type of parameters");
                    System.exit(1);
                }
            }
            child_method_contents.override_method = true;
        }
        //child_method_contents.override_method = true;
        return true;
    }


    public void printOffsets () {
        int fields_offset_sum = 0;
        int methods_offset_sum = 0;
        for (Map.Entry<String, ClassContents> class_entry : symbol_table.entrySet()) {
            String class_key = class_entry.getKey();
            ClassContents current_class_contents = class_entry.getValue();

            if (current_class_contents.parent_class==null) {
                fields_offset_sum = 0; methods_offset_sum = 0;
            }

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
                        //TODO
                        //System.err.println("wrong type: " + current_field_type);
                        //System.exit(1);
                        current_offset = 8;
                }
                System.out.println(class_key + '.' + field_key + " : " + fields_offset_sum);
                fields_offset_sum += current_offset;
            }
            for (Map.Entry<String, MethodContents> method_entry : current_class_contents.methods.entrySet()) {
                String method_key = method_entry.getKey();
                MethodContents current_method_contents = method_entry.getValue();
                if (!current_method_contents.override_method) {
                    System.out.println(class_key + '.' + method_key + " : " + methods_offset_sum);
                    methods_offset_sum += 8;
                }
            }
        }
    }

    public String getTypeofIdentifier (String id, String class_name, String method_name) {
        ClassContents class_contents = symbol_table.get(class_name);
        if (class_contents == null) {
            System.err.println("Cannot find class " + class_name + " in symbol table");
            System.exit(1);
        }
        if (class_contents.fields.containsKey(id)) return class_contents.fields.get(id);

        MethodContents method_contents = class_contents.methods.get(method_name);
        if (method_contents == null) {
            System.err.println("Cannot find method " + method_name + " in class " + class_name);
            System.exit(1);
        }

        if (method_contents.parameters.containsKey(id)) return method_contents.parameters.get(id);
        if (method_contents.variables.containsKey(id)) return method_contents.variables.get(id);

        while (class_contents.parent_class != null) {
            ClassContents parent_contents = symbol_table.get(class_contents.parent_class);
            if (parent_contents.fields.containsKey(id)) return parent_contents.fields.get(id);
            class_contents = parent_contents;
        }
        return "";
    }

    //add parameters in a method
    public boolean addParameters (String class_name, String method_name, String param_type, String param_name) {
        ClassContents content = symbol_table.get(class_name);
        if (content == null) {
            System.err.println("Cannot find class " + class_name + " in symbol table");
            System.exit(1);
        }

        content.addParameters (method_name, param_type, param_name);
        return true;
    }

    public String getReturnType (String class_name, String method_name) {
        ClassContents content = symbol_table.get(class_name);
        if (content == null) {
            System.err.println("Cannot find class " + class_name + " in symbol table");
            System.exit(1);
        }

        MethodContents method_contents = content.methods.get(method_name);
        return method_contents.return_type;
    }

    public boolean isParentType (String class_name, String type) {
        ClassContents class_contents = symbol_table.get(class_name);
        if (class_contents == null) return false;

        while (class_contents.parent_class != null) {
            if (class_contents.parent_class.equals(type)) return true;
            class_contents = symbol_table.get(class_contents.parent_class);
            //class_contents = parent_contents;
        }
        return false;
    }
}

class ClassContents {
    LinkedHashMap <String, String> fields;
    LinkedHashMap <String, MethodContents> methods;
    String class_name;
    String parent_class;

    public String getParentClass () {
        return parent_class;
    }

    public ClassContents (String given_class_name, String given_parent_class) {
        fields = new LinkedHashMap<String, String>();
        methods = new LinkedHashMap<String, MethodContents>();
        class_name = given_class_name;
        parent_class = given_parent_class;
    }

    public boolean fieldsContains (String member_name) {
        return fields.containsKey(member_name);
    }

    public boolean methodsContains (String name) {
        return methods.containsKey(name);
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
        if (methodsContains(method_name)) return false;
        MethodContents content = new MethodContents (method_name, return_type);
        if (methods.putIfAbsent (method_name, content) == null) return true;
        else return false;
    }

    public boolean addParameters (String method_name, String param_type, String param_name) {
        MethodContents method_contents = methods.get(method_name);
        //if function doesn't exist in parent class, then no override problem

        if (method_contents==null) {
            System.err.println("Method " + method_name + " isn't declared.");
            return false;
        }
        method_contents.parametersAdd (param_type, param_name);
        return true;
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

    public String getReturnType () {
        return return_type;
    }

    public boolean parametersContains (String name) {
        return parameters.containsKey(name);
    }

    public boolean variablesContains (String name) {
        return variables.containsKey(name);
    }

    public boolean parametersAdd (String type, String name) {
        if (parameters.putIfAbsent (name, type) == null) return true;
        else return false;
    }

    public boolean variablesAdd (String type, String name) {
        if (variables.putIfAbsent (name, type) == null) return true;
        else return false;
    }
}
