package types;

import java.util.*;

public class MethodContents {
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