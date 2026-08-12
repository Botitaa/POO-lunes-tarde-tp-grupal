public class SantinoFaccioClase2 {

    public static void main(String[] args) {
        System.out.println(Ejercicio1(3, 8, 11));
        System.out.println(Ejercicio2(8));
        System.out.println(Ejercicio3(5)); // valores d ejemplo
    }

    public static double[] Ejercicio1(int a, int b, int c) {
        double raiz = (Math.pow(b, 2) - 4 * a * c);
        raiz = Math.sqrt(raiz);
        double r1 = (-b + raiz) / (2 * a);
        double r2 = (-b - raiz) / (2 * a);
        return new double[]{r1, r2};
    }

    public static int Ejercicio2(int mes) {
        String[] meses = {"enero", "febrero", "marzo", "abril", "mayo", "junio",
                "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};

        int[] dias = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

        if (mes >= 1 && mes <= 12) {
            return dias[mes - 1];
        }
        return -1; //cuando le mes no existe.
    }

    public static boolean Ejercicio3(int num) {
        return num % 4==0;
    }
}