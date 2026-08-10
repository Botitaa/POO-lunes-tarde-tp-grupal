public class Main {

    public static double[] raices(int a, int b, int c){
        //hallar raices de una cuadratica
        //-b+-raiz(b^2-4ac)/2a o formula de barskara

        double interiorRaiz = (b*b-4*a*c);
        double raiz = Math.sqrt(interiorRaiz);
        double resultado1 = ((-b) + raiz)/(2 * a);
        double resultado2 = ((-b) - raiz)/(2 * a);

        return new double[] {resultado1, resultado2};
    }


    public static String diasMes(int numeroMes){

        String[] meses = {"enero", "febrero", "marzo", "abril", "mayo", "junio",
                "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};

        int[] dias = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

        return meses[numeroMes - 1] + " tiene: " + dias[numeroMes - 1];

    }

    public static boolean anioBisiesto(int anio){
        return anio%4 == 0;
    }


    public static void main(String[] args) {
        // Raíces de una cuadrática
        double[] r = raices(1, -3, 2);
        System.out.println("Raices: " + r[0] + " y " + r[1]);

        // Días del mes 
        System.out.println(diasMes(2));

        // Año bisiesto
        System.out.println("2024 es bisiesto? " + anioBisiesto(2024));
        System.out.println("2023 es bisiesto? " + anioBisiesto(2023));
        System.out.println("1900 es bisiesto? " + anioBisiesto(1900));     
    }

}
