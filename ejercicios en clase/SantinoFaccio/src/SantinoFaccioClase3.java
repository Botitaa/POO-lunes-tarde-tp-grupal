public class SantinoFaccioClase3 {
    public static void main(String[] args) {
        System.out.println(Ejercicio1(5));
        Ejercicio2(7);
    }
    public static double Ejercicio1(int n){
        double i = 1.0;
        double total = 0;
        while (i <= n){
            total += 1.0 / i;
            i++;
        }
        return total;
    }
    public static void Ejercicio2(int n){
        int f1 = 0;
        int f2 = 1;
        int recorrido = 2;
        if(n >= 0){
            System.out.println(0);
        }
        if(n >= 1){
            System.out.println(1);
        }
        while(recorrido < n){
            int aux = f1; //para que no se sobreescriba a la hora deasignarle a f2
            System.out.println(f1+f2);
            f1 = f2;
            f2 = aux+f2;
            recorrido++;
        }
    }

}
