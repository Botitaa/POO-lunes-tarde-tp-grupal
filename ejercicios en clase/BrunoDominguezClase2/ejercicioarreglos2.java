public class ejercicioarreglos2 {
    public static void main(String[] args) {
        int[] array = {4, 12, 7, 25, 9, 3};
        int buscado = 25;
        boolean encontrado = false;
        int posicion = -1;

        for (int i = 0; i < array.length; i++) {
            if (array[i] == buscado) {
                encontrado = true;
                posicion = i;
            }
        }

        if (encontrado) {
            System.out.println(buscado + " esta en la posicion " + posicion);
        } else {
            System.out.println(buscado + " no esta en el array");
        }
    }
}
