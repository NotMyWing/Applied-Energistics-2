package appeng.client.gui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Stack;

public class MathExpressionParser {

    private final Stack<String> postfixStack = new Stack<>();
    private final Stack<Character> opStack = new Stack<>();
    private static final int[] OPERATOR_PRIORITY = new int[] { 0, 3, 2, 1, -1, 1, 0, 2 };

    public static double parse(String expression) {
        double result;

        if (expression == null) return Double.NaN;

        expression = expression.replace(" ", "");

        if (expression.length() == 1 && Character.isDigit(expression.charAt(0))) {
            return expression.charAt(0) - '0';
        }
        try {
            expression = transform(expression);
            result = new MathExpressionParser().calculate(expression);
        } catch (Exception e) {
            return Double.NaN;
        }
        return result;
    }

    /**
     * replace '-' with '~'
     * e.g.-2+-1*(-3E-2)-(-1) -> ~2+~1*(~3E~2)-(~1)
     */
    private static String transform(String expression) {
        char[] arr = expression.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == '-') {
                if (i == 0) {
                    arr[i] = '~';
                } else {
                    char c = arr[i - 1];
                    if (c == '+' || c == '-' || c == '*' || c == '/' || c == '(' || c == 'E' || c == 'e') {
                        arr[i] = '~';
                    }
                }
            }
        }
        if (arr[0] == '~' || arr[1] == '(') {
            arr[0] = '-';
            return "0" + new String(arr);
        } else {
            return new String(arr);
        }
    }

    public double calculate(String expression) {
        Stack<String> resultStack = new Stack<>();
        prepare(expression);
        Collections.reverse(postfixStack);
        String firstValue, secondValue, currentValue;
        while (!postfixStack.isEmpty()) {
            currentValue = postfixStack.pop();
            if (!isOperator(currentValue.charAt(0))) {
                currentValue = currentValue.replace("~", "-");
                resultStack.push(currentValue);
            } else {
                secondValue = resultStack.pop();
                firstValue = resultStack.pop();

                firstValue = firstValue.replace("~", "-");
                secondValue = secondValue.replace("~", "-");

                String tempResult = calculate(firstValue, secondValue, currentValue.charAt(0));
                resultStack.push(String.valueOf(tempResult));
            }
        }
        return Double.parseDouble(resultStack.pop());
    }

    private void prepare(String expression) {
        opStack.push(',');
        char[] arr = expression.toCharArray();
        int currentIndex = 0;
        int count = 0;
        char currentOp, peekOp;
        for (int i = 0; i < arr.length; i++) {
            currentOp = arr[i];
            if (isOperator(currentOp)) {
                if (count > 0) {
                    postfixStack.push(new String(arr, currentIndex, count));
                }
                peekOp = opStack.peek();
                if (currentOp == ')') {
                    while (opStack.peek() != '(') {
                        postfixStack.push(String.valueOf(opStack.pop()));
                    }
                    opStack.pop();
                } else {
                    while (currentOp != '(' && peekOp != ',' && compare(currentOp, peekOp)) {
                        postfixStack.push(String.valueOf(opStack.pop()));
                        peekOp = opStack.peek();
                    }
                    opStack.push(currentOp);
                }
                count = 0;
                currentIndex = i + 1;
            } else {
                count++;
            }
        }
        if (count > 1 || (count == 1 && !isOperator(arr[currentIndex]))) {
            postfixStack.push(new String(arr, currentIndex, count));
        }

        while (opStack.peek() != ',') {
            postfixStack.push(String.valueOf(opStack.pop()));
        }
    }

    private static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '(' || c == ')';
    }

    private static boolean compare(char cur, char peek) {
        return OPERATOR_PRIORITY[(peek) - 40] >= OPERATOR_PRIORITY[(cur) - 40];
    }

    private String calculate(String firstValue, String secondValue, char currentOp) {
        return switch (currentOp) {
            case '+' -> add(firstValue, secondValue);
            case '-' -> sub(firstValue, secondValue);
            case '*' -> mul(firstValue, secondValue);
            case '/' -> div(firstValue, secondValue);
            default -> "";
        };
    }

    public static String add(String v1, String v2) {
        BigDecimal b1 = new BigDecimal(v1);
        BigDecimal b2 = new BigDecimal(v2);
        return String.valueOf(b1.add(b2));
    }

    /**
     * subtraction
     *
     * @param v1 p1
     * @param v2 p2
     * @return sub
     */
    public static String sub(String v1, String v2) {
        BigDecimal b1 = new BigDecimal(v1);
        BigDecimal b2 = new BigDecimal(v2);
        return String.valueOf(b1.subtract(b2));
    }

    /**
     * multiplication
     *
     * @param v1 p1
     * @param v2 p2
     * @return mul
     */
    public static String mul(String v1, String v2) {
        BigDecimal b1 = new BigDecimal(v1);
        BigDecimal b2 = new BigDecimal(v2);
        return String.valueOf(b1.multiply(b2));
    }

    /**
     * division. e = 10^-10
     *
     * @param v1 p1
     * @param v2 p2
     * @return div
     */
    public static String div(String v1, String v2) {
        BigDecimal b1 = new BigDecimal(v1);
        BigDecimal b2 = new BigDecimal(v2);
        return String.valueOf(b1.divide(b2, 16, RoundingMode.HALF_UP));
    }

    /**
     * rounding
     *
     * @param v     p
     * @param scale scale
     * @return result
     */
    public static double round(double v, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("The scale must be a positive integer or zero");
        }
        BigDecimal b = new BigDecimal(Double.toString(v));
        return b.divide(BigDecimal.ONE, scale, RoundingMode.HALF_UP).doubleValue();
    }

    public static String round(String v, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("The scale must be a positive integer or zero");
        }
        BigDecimal b = new BigDecimal(v);
        return String.valueOf(b.divide(BigDecimal.ONE, scale, RoundingMode.HALF_UP));
    }
}
