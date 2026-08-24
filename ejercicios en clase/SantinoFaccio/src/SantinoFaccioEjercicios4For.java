public class SantinoFaccioEjercicios4For {
    public static void main(String[] args){
        System.out.println(Ejercicio1(5));
        System.out.println(Ejercicio2(20,10));
    }
    public static int Ejercicio1(int n){
        int resultado = 1;
        for(int i = n; i >= 2;i--){
            resultado = resultado * i;
        }
        return resultado;
    }
    public static int Ejercicio2(int n1,int n2){
        int MDC = maximoDivisorComun(n1,n2);
        return MDC;
    }

    private static int maximoDivisorComun(int n1,int n2){
        int max = 1;
        if (n1>=n2){//osea si n1 es mas grande o igual que llegue hasta n2 nomas el fpr
            for(int i = 2;i <= n2;i++){
                if(n1 % i == 0 && n2 % i == 0){
                    max = i;
                }
            }
        }
        else{ // si n2 es mas grande
            for(int i = 2;i <= n1;i++){
                if(n1 % i == 0 && n2 % i == 0){
                    max = i;
                }
            }
        }
        return max;
    }

}
