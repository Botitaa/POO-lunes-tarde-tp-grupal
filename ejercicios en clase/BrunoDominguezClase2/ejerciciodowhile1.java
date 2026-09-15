public class ejerciciodowhile1 {
    public static void main(String[] args) {
        int numero = 4520;
        int copia = numero;
        int contador = 0;

        do {
            copia = copia / 10;
            contador++;
        } while (copia > 0);

        System.out.println(numero + " tiene " + contador + " digitos");
    }
}
