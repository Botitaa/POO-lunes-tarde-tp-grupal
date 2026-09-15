public class ejerciciowhile2 {
    public static void main(String[] args) {
        int n = 10;
        int anterior = 0;
        int siguiente = 1;
        int contador = 0;

        while (contador < n) {
            System.out.println(anterior);
            int nuevo = anterior + siguiente;
            anterior = siguiente;
            siguiente = nuevo;
            contador++;
        }
    }
}
