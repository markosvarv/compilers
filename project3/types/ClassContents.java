package types;

import java.util.*;

public class ClassContents {
    LinkedHashMap<String, String> fields;
    LinkedHashMap <String, MethodContents> methods;
    String class_name;
    String parent_class;
    boolean is_main;
    int fields_offset_sum;
    int methods_offset_sum;

    public ClassContents (String given_class_name, String given_parent_class, boolean main_class) {
        fields = new LinkedHashMap<>();
        methods = new LinkedHashMap<>();
        class_name = given_class_name;
        parent_class = given_parent_class;
        is_main = main_class;
    }

    public boolean getIsMain() {
        return is_main;
    }

    public String getClassName() {
        return class_name;
    }

    public Collection<MethodContents> getMethodContents() {
        return methods.values();
    }

    public String getParentClass () {
        return parent_class;
    }

    public boolean VarAdd (String type, String member_name) {
        return fields.putIfAbsent(member_name, type) == null;
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
        return methods.putIfAbsent(method_name, new MethodContents(method_name, return_type)) == null;
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
