

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class ppc {

  // ----------- Config --------------
  static int nBox = 3; // Numero de Boxes
  static int nPessoas = 60; // Numero de Pessoas

  final static Lock lock = new ReentrantLock(); // Trava
  final static banheiro b = new banheiro(nBox);
  final static List pessoas = new ArrayList();
  final static List fila = new ArrayList();
  static List foram = new ArrayList();
  static boolean novoGenero = false;

  public static void esperar_ultimo() {
    try {
      TimeUnit.SECONDS.sleep(6);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    for (int i = 0; i < nPessoas; i++) {
      pessoa p = new pessoa(i%3);
      pessoas.add(p);
    }
    long inicio_da_aplicacao = System.currentTimeMillis();
    while (true) {
      Random r = new Random();
      pessoa p = null;
      if (pessoas.size() > 0) { // Verifica se todos ja foram para a fila
        ppc.lock.lock();
        p = (pessoa) pessoas.get(r.nextInt(pessoas.size()));
        if(b.genero == -1) b.genero = p.genero;
        p.setName("" + foram.size());
        p.hora_chegada = System.currentTimeMillis();
        fila.add(p);
        foram.add(p);
        pessoas.remove(p);
        ppc.lock.unlock();
        b.next();
      }

      System.out.print("\n\n\n");
      System.out.println("\nFALTAM ENTRAR NA FILA: " + pessoas.size() + 
      " \nQUANTIDADE DE PESSOAS NA FILA " + fila.size() 
      + " \nGENERO ATUAL DO BANHEIRO "  + b.genero);
      

      if (pessoas.size() == 0 && fila.size() == 0) { // Se todos ja foram ao banheiro
        break;
      }

      if (fila.size() > 0 && foram.size() < (nPessoas - 1) && pessoas.size() == 0) {
        ppc.lock.unlock();
        b.next();
      }
      try {
        TimeUnit.SECONDS.sleep(r.nextInt(6) + 1); // Tempo nova pessoa
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    }
    ppc.esperar_ultimo(); // Espera o ultimo terminar de ir no banheiro
    long tempo_total =  ((System.currentTimeMillis() - inicio_da_aplicacao)/1000);
    double vetPessoa[] = new double[3];
    for(int i = 0; i< nPessoas; i++){ // Print pessoas e tempo
      pessoa p = (pessoa)foram.get(i);
      vetPessoa[p.genero] = vetPessoa[p.genero] + p.tempo;
      System.out.println(p.getName() + " - "+ p.tempo);
    }
    for( int i = 0; i<nBox; i++){ // Printa tempo das box
    	double t = (double)b.box_time[i] / (double) tempo_total;
      System.out.println("Box: "+i +" - "+ t);
    }
    System.out.println("Tempo utilizado por genero");
    System.out.println("Genero 0 - " + vetPessoa[0]);
    System.out.println("Genero 1 - " + vetPessoa[1]);
    System.out.println("Genero 2 - " + vetPessoa[2]);
  }

}

class banheiro {
  public boolean status = false; // true = ocupado totalmente (LOTADO)
  public int genero = -1; // Genero atual do banheiro
  public boolean box[]; // true = em_uso
  public long box_time[]; // tempo de cada box

  public banheiro(int box_num) {
    box = new boolean[box_num];
    box_time = new long[box_num];
  }

  public synchronized void next() { // Verifica quem é o proximo
    ppc.lock.lock();
    pessoa p = null;
    if(!ppc.fila.isEmpty()){
      p = (pessoa) ppc.fila.get(0);
    } // Para nao ser nulo pegar a primeira pessoa da fila
    else{ // caso nao tenha ninguem esperando libera a trava
      ppc.lock.unlock();
      return;
    }
    if ( (vazio() || !ppc.novoGenero ) && !this.status  ) { // Banheiro Vazio e ninguem de outro genero para entrar
      try {
        if(vazio()) this.genero = p.genero;
        for (int i = 0; i < ppc.fila.size(); i++) { // Pega todas as pessoas possiveis para entrar, levando em conta quantos box livre tem
          pessoa p2 = (pessoa) ppc.fila.get(i);

          if (this.genero > -1 && (p2.genero == this.genero) && !p2.isAlive()) {
            p = p2;
            for (int j = 0; j < ppc.nBox; j++) {
              if (!this.box[j]) { // Se box livre bota a pessoa
                this.box[j] = true;
                p.box = j;
                try{
                  ppc.fila.remove(p);
                  i--;
                  p.start();
                  break;
                }catch(IllegalThreadStateException exception){
                }
              }
            }
            
          }
          vazio();
          if(this.status){break;} // Se lotou sai da execucao
        }
      } finally {
      }
    } else if (this.genero != p.genero && !ppc.novoGenero) { // Novo genero e nenhum outro novo genero antes
      ppc.novoGenero = true;
      this.genero = p.genero;
 
    } else { // Algum outro caso, analisando...

    }
    ppc.lock.unlock();
  }

  boolean vazio(){ // Verifica se os box estao vazios e informa se esta lotado ou nao
    int j = 0;
    for (int i = 0; i < ppc.nBox; i++) {
      
      if (this.box[i]) {
        j++;
        //return false;
      }
    }
    if(j == ppc.nBox){
      this.status = true;
    }else{
      this.status = false;
    }
    if(j==0){
      ppc.novoGenero = false;
    }

    return j==0;
  }
}

class pessoa extends Thread {
  public long hora_chegada;
  public int genero;
  public int box; // Em que box entrou
  public long tempo; // Tempo total na fila

  public pessoa(int genero) {
    this.genero = genero;
  }

  public void run() {

    try {
      TimeUnit.SECONDS.sleep(5); // usa banheiro
      this.tempo = (System.currentTimeMillis()/1000 - this.hora_chegada/1000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    ppc.b.box[box] = false; // Libera o box
    ppc.b.box_time[box] += 5.0; // Adiciona 5 segundos de uso do box
    ppc.b.next();
//      soFila.signal();
  }
}
