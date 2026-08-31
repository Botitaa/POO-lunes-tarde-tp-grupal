import java.util.Scanner;

public class ActividadTemperaturasClase3 {
    public static void main(String[] args){
        //arrays, scanner .
        Scanner scanner = new Scanner(System.in);
        double[] temperaturas = new double[7];
        String[] dias = new String[]{"lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo"};

        System.out.println("1.Cargar las 7 temperaturas del arreglo.");
        for(int i = 0; i < temperaturas.length;i++){
            double grados = scanner.nextDouble();
            temperaturas[i] = grados;
        }
            //SE CARGAN LAS 7 TEMPERATURAS ANTES DE SEGUIR CON EL PROGRAMA!
        while (true) {
            //bucle de opciones hasta q pongan 0.
            System.out.println("2.Calcular e imprimir el promedio de la semana.");
            System.out.println("3.Encontrar la temperatura máxima y la mínima.");
            System.out.println("4.Contar cuántos días superan un umbral dado");
            System.out.println("5.Recorrer el arreglo usando length e imprimir cada día con su temperatura.");
            System.out.println("0.Salir");

            System.out.println("elegir op");
            int op = scanner.nextInt();

            if (op == 2) {
                System.out.println("la temperatura promedio es: " +
                        promedio(temperaturas));
            }
            else if (op == 3) {
                System.out.println("la temperatura maxima es: " + tempMaxima(temperaturas)
                        + " la temperatura minima es: " + tempMinima(temperaturas));
            }
            else if (op == 4) {
                System.out.println("ingrese su umbral: ");
                double umrbal = scanner.nextDouble();
                System.out.println(diasQueSuperanElUmbral(temperaturas, umrbal) +
                        " superan el umbral");
            }
            else if (op == 5) {
                recorrerSemanaPrintear(temperaturas, dias);
            }
            else if (op == 0) {
                return;
            }
            else {
                System.out.println("opcion incorrecta.");
            }
        }
    }

    private static void recorrerSemanaPrintear(double[] temperaturas,String[] dias){
        for(int i = 0;i < dias.length;i++){
            System.out.println("dia: " + dias[i] + " temperatura: " + temperaturas[i]);
            //recorre ambos arrays y printea la data
        }
    }

    private static int diasQueSuperanElUmbral(double[] temperaturas, double umbral){
        int contador = 0;
        for(int i = 0; i < temperaturas.length;i++){
            if(temperaturas[i] > umbral){
                contador++;
            }
        }
        return contador;
        //recibe el umbral que quiere el user y hace un contador de los dias q superan
        //la temp
    }

    private static double promedio(double[] temperaturas){
        double temp = 0;
        for(int i = 0; i < temperaturas.length;i++){
            temp += temperaturas[i];
        }
        return temp / 7;
        //suma todas las temps y las divide por la cantidad d dias
    }

    private static double tempMaxima(double[] temperaturas){
        double max = temperaturas[0];
        for (int i = 1; i < temperaturas.length;i++){
            if (temperaturas[i] > max){
                max = temperaturas[i];
            }
        }
        return max;
        //calcula la temp maxima, se asigna por default la del lunes y se suplanta por la mas alta.
    }

    private static double tempMinima(double[] temperaturas){
        double min = temperaturas[0];
        for (int i = 1; i < temperaturas.length;i++){
            if (temperaturas[i] < min){
                min = temperaturas[i];
            }
        }
        return min;
        //misma logica que el anterior con el lunes, pero buscando la temp mas baja!!!!
    }
}
