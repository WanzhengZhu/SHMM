package myutils;

/**
 * Functions that are not part of standard libraries
 * User: Michael
 * Date: 1/9/12
 * Time: 9:22 PM
 */
public class BesselI {

    public static final double ACC = 4.0;
    public static final double BIGNO = 1.0e10;
    public static final double BIGNI = 1.0e-10;

    public static void main(String[] args) {
        double xmin = ((args.length > 0) ? Double.valueOf(args[0]) : 0.0);
        double xmax = ((args.length > 1) ? Double.valueOf(args[1]) : 4.0);
        double dx = ((args.length > 2) ? Double.valueOf(args[2]) : 0.1);
        System.out.printf("%10s %10s %10s %10s\n", "x", "bessi0(x)", "bessi1(x)", "bessi2(x)");
        for (double x = xmin; x < xmax; x += dx) {
            System.out.printf("%10.6f %10.6f %10.6f %10.6f\n", x, bessi0(x), bessi1(x), bessi(2, x));
        }
    }

    public static final double bessi0(double x) {
        double answer;
        double ax = Math.abs(x);
        if (ax < 3.75) { // polynomial fit
            double y = x / 3.75;
            y *= y;
            answer = 1.0 + y * (3.5156229 + y * (3.0899424 + y * (1.2067492 + y * (0.2659732 + y * (0.360768e-1 + y * 0.45813e-2)))));
        } else {
            double y = 3.75 / ax;
            answer = 0.39894228 + y * (0.1328592e-1 + y * (0.225319e-2 + y * (-0.157565e-2 + y * (0.916281e-2 + y * (-0.2057706e-1 + y * (0.2635537e-1 + y * (-0.1647633e-1 + y * 0.392377e-2)))))));
            answer *= (Math.exp(ax) / Math.sqrt(ax));
        }
        return answer;
    }

    public static final double bessi1(double x) {
        double answer;
        double ax = Math.abs(x);
        if (ax < 3.75) { // polynomial fit
            double y = x / 3.75;
            y *= y;
            answer = ax * (0.5 + y * (0.87890594 + y * (0.51498869 + y * (0.15084934 + y * (0.2658733e-1 + y * (0.301532e-2 + y * 0.32411e-3))))));
        } else {
            double y = 3.75 / ax;
            answer = 0.2282967e-1 + y * (-0.2895312e-1 + y * (0.1787654e-1 - y * 0.420059e-2));
            answer = 0.39894228 + y * (-0.3988024e-1 + y * (-0.362018e-2 + y * (0.163801e-2 + y * (-0.1031555e-1 + y * answer))));
            answer *= (Math.exp(ax) / Math.sqrt(ax));
        }
        return answer;
    }

    public static final double bessi(int n, double x) {
        if (n < 2)
            throw new IllegalArgumentException("Function order must be greater than 1");
        if (x == 0.0) {
            return 0.0;
        } else {
            double tox = 2.0/Math.abs(x);
            double ans = 0.0;
            double bip = 0.0;
            double bi  = 1.0;
            for (int j = 2*(n + (int)Math.sqrt(ACC*n)); j > 0; --j) {
                double bim = bip + j*tox*bi;
                bip = bi;
                bi = bim;
                if (Math.abs(bi) > BIGNO) {
                    ans *= BIGNI;
                    bi *= BIGNI;
                    bip *= BIGNI;
                }
                if (j == n) {
                    ans = bip;
                }
            }
            ans *= bessi0(x)/bi;
            return (((x < 0.0) && ((n % 2) == 0)) ? -ans : ans);
        }
    }
}