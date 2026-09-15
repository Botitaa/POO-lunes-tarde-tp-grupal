public class ejercicio1 {
    public static void main(String[] args){
        int a = 1;
        int b = -3;
        int c = 2;
        int discriminante = b * b - 4 * a * c;
        if (discriminante > 0) {
            System.out.println("Hay dos raices reales distintas");
        } else if (discriminante == 0) {
            System.out.println("Hay una raiz real doble");
        } else {
            System.out.println("No hay raices reales (raices complejas)");
        }
    }
}
