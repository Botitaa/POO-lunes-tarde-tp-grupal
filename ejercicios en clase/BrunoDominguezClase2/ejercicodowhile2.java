public class ejerciciodowhile2 {
    public static void main(String[] args) {
        double nota1 = 4.5;
        double nota2 = 2.8;
        double nota3 = 3.0;
        double nota4 = 1.5;
        double nota5 = 3.8;

        int aprobadas = 0;
        int i = 1;
        double notaActual;

        do {
            if (i == 1) {
                notaActual = nota1;
            } else if (i == 2) {
                notaActual = nota2;
            } else if (i == 3) {
                notaActual = nota3;
            } else if (i == 4) {
                notaActual = nota4;
            } else {
                notaActual = nota5;
            }

            if (notaActual >= 3.0) {
                aprobadas++;
            }

            i++;
        } while (i <= 5);

        System.out.println("Notas mayores o iguales a 3.0: " + aprobadas);
    }
}
