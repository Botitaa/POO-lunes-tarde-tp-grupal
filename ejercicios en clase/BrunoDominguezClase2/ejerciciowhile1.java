public class ejerciciowhile1 {
    public static void main(String[] args) {
        int n = 5;
        int i = 1;
        double suma = 0;

        while (i <= n) {
            suma = suma + 1.0 / i;
            i++;
        }

        System.out.println("Suma = " + suma);
    }
}
