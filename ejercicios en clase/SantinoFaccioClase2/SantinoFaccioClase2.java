
public class SantinoFaccioClase2 {
    public static void main(String[] args){
     Ejercicio1(3,8,11);
     Ejercicio2(8);
     Ejercicio3(5);//valor de ejemplo
    }
    public static double[] Ejercicio1(int a, int b,int c){
        double raiz = (Math.pow(b, 2) -4 * a * c);
        raiz = Math.sqrt(raiz);
        double r1 = (-b + raiz) / (2 * a);
        double r2 = (-b - raiz) / (2 * a);
        return new double[]{r1, r2};
    }

    public static int Ejercicio2(int mes){
        String[] meses = {"enero", "febrero", "marzo", "abril", "mayo", "junio",
                "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};

        int[] dias = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        int recorrido = 1;
        while(recorrido < meses.length){
            if i == mes{
                return dias[i]
            }
            recorrido++;
        }
        return -1 // el mes no existe

    }

    public static boolean Ejercicio3(int num){
        if(num % 4 == 0){
            return true;
        }
        return false;
    }

}