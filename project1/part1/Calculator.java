import java.io.InputStream;
import java.io.IOException;

public class Calculator {

    private int lookaheadToken;

    private InputStream in;

    public Calculator(InputStream in) throws IOException {
        this.in = in;
        lookaheadToken = in.read();
    }

    private void consume(int symbol) throws IOException, ParseError {
        if (lookaheadToken != symbol){
            throw new ParseError();
        }
        lookaheadToken = in.read();
    }

    private int evalDigit(int digit){
        return digit - '0';
    }

    private int Exp() throws IOException, ParseError {
        int value = Term();
        return Exp2(value);
    }

    private int Exp2(int value1) throws IOException, ParseError {
        if (lookaheadToken == -1 || lookaheadToken == '\n' || lookaheadToken == ')')
            return value1;
        if (lookaheadToken != '+' && lookaheadToken != '-'){
            throw new ParseError();
        }
        int old_token = lookaheadToken;
        lookaheadToken = in.read(); //consume character

        int value2 = Term();

        int newvalue = old_token == '+' ? value1 + value2 : value1 - value2;
        return Exp2(newvalue);
    }

    private int Term() throws IOException, ParseError {
        int value = Factor();
        return Term2(value);
    }

    private int Term2(int value1) throws IOException, ParseError {
        if(lookaheadToken == '+' || lookaheadToken == '-' || lookaheadToken == '\n' || lookaheadToken == -1 || lookaheadToken == ')')
            return value1;
        if (lookaheadToken != '*' && lookaheadToken != '/'){
            throw new ParseError();
        }
        int old_token = lookaheadToken;
        lookaheadToken = in.read();
        int value2 = Factor();

        int newvalue = old_token == '*' ? value1 * value2 : value1 / value2;
        return Term2(newvalue);
    }

    private int Factor() throws IOException, ParseError {
        if (lookaheadToken == '(') {
            consume('(');
            int result = Exp();
            consume(')');
            return result;
        }
        else if (lookaheadToken < '0' || lookaheadToken > '9') {
            throw new ParseError();
        }

        return Num();
    }

    private int Num() throws IOException, ParseError {
        if(lookaheadToken < '0' || lookaheadToken > '9'){
            throw new ParseError();
        }

        int value1 = evalDigit(lookaheadToken);
        consume(lookaheadToken);

        int value2 = Num2(value1);
        if (value2 == -1) return value1;
        else return Integer.parseInt(Integer.toString(value1) + Integer.toString(value2));
    }

    private int Num2(int value) throws IOException, ParseError {
        if(lookaheadToken == '+' || lookaheadToken == '-' || lookaheadToken == '*' || lookaheadToken == '/'
                || lookaheadToken == '\n' || lookaheadToken == -1 || lookaheadToken == ')') {
            return -1;
        }
        if(lookaheadToken < '0' || lookaheadToken > '9'){
            throw new ParseError();
        }
        return Num();
    }


    public int eval() throws IOException, ParseError {
        int result = Exp();
        if (lookaheadToken != '\n' && lookaheadToken != -1){
            throw new ParseError();
        }
        return result;
    }

    public static void main(String[] args) {
        try {
            Calculator evaluate = new Calculator(System.in);
            //System.out.println((17+21)/3*33+1-375-1/2+3343345*(1+24*3/2-1+1)/(1+2*(24545345*2135/454253-(26*3))-12+((14))));
            System.out.println((17+21)/3*33+1-375-1/2+3343345*(1+24*3/2-1+1)/(1+2*(24545345*2135/454253-(26*3))-12+((14))));

            System.out.println(evaluate.eval());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (ParseError err) {
            System.err.println(err.getMessage());
        }
    }
}
