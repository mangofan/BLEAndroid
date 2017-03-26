package code.source.es.newbluetooth.Activity;

/**
 * Created by fanwe on 2017/1/10.
 */

public class test {

    public static void main(String[] args) {

        // get two double numbers numbers
        double x = 45;
        double y = -180;

        // convert them in degrees
        x = Math.toRadians(x);
        y = Math.toRadians(y);

        // print the hyperbolic tangent of these doubles
        System.out.println("Math.tanh(" + x + ")=" + Math.toDegrees(x));
        System.out.println("Math.tanh(" + y + ")=" + Math.toDegrees(y));

    }
}