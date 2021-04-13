import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Arrays;

import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.JPanel;


public class Bilardo extends JPanel implements Runnable, MouseListener, MouseMotionListener {
    
    // durumlar
    public final int WAITING_TO_START = 0;
    public final int WAITING_TO_HIT_THE_BALL = 1;
    public final int MOVING = 2;
    public final int FINISHING = 3;
    
    public int state = 0;
    
    // masa
    double hR;
    double[] tableX;
    double[] tableY;
    double[] holesX;
    double[] holesY;

    // toplar
    public int tumtoplar;
    public int renkli_toplar;
    double[] x; //toplarin x'i
    double[] y; //toplarin y'si
    double[] vx; 
    double[] vy;
    double[] nextX;
    double[] nextY;
    double[] next_vx;
    double[] next_vy;
    boolean[] borderCollision;
    boolean[][] collision;
    boolean[] top_masadami;
    double ballradius = 10;
    
    // animasyon filan
    Image backBuffer;
    Image backGround;
    
    // mouse
    int mouseX;
    int mouseY;
    int mouseXsonraki;
    int mouseYsonraki;
    boolean clicked;
    
    // ıstaka
    public final int MAX_STRENGTH = 1000;
    int stickLength = 150;
    
    public Bilardo() {
        super();
        this.setBounds(50, 50, 640, 480);
        //this.setResizable(false);
        //this.setUndecorated(true);
        //this.setVisible(true);

        JFrame f = new JFrame("Billard Game");        
        f.add(this);
        f.setBounds(0, 0, 700, 380);
        f.setResizable(false);
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        this.requestFocus();
        
        init();
    }
    
    public void init() {
        
        initTable();
        initBalls();
        
        backBuffer = this.createImage(this.getWidth(), this.getHeight());
        //gBackBuffer = backBuffer.getGraphics();
        //gBackBuffer.setFont(new Font("Courier", Font.BOLD, 20));
        
        createBackGround();

        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        
        start();
    }
    
    
    public void initTable() {                
        tableX = new double[] 
          {40,650};     //               {40,this.getWidth()-40}; 
        
        tableY = new double[] 
          {40, 300};//          {tableX[0], this.getHeight()-tableX[0]};

        //this.getHeight()-60
        holesX = new double[]
          {60, 345, 630 };  //{tableX[0] + 20, this.getWidth()/2, tableX[1]-20 };
        
        holesY = new double[]
          {60, 280 }; // {tableY[0] + 20, this.tableY[1]-20 };  
    }
    
    
    public void initBalls() {
        tumtoplar = 3;
        x = new double[tumtoplar];         //topların x'leri
        y = new double[tumtoplar];         //topların y'leri
        vx = new double[tumtoplar];        //topların x'teki hızları    
        vy = new double[tumtoplar];        //topların y'deki hızları
        nextX = new double[tumtoplar];     //topların x'teki next pozisyonları
        nextY = new double[tumtoplar];     //topların y'deki next pozisyonları
        next_vx = new double[tumtoplar];   
        next_vy = new double[tumtoplar];
        borderCollision = new boolean[tumtoplar];
        collision = new boolean[tumtoplar][tumtoplar]; 
        top_masadami = new boolean[tumtoplar];  //masadaki top sayısına göre 
        
        setBalls();
        setWhiteBall();    
        
    }
    
    
    public void setWhiteBall() {
        x[0] =180;   
        y[0] = this.getHeight() / 2;
        vx[0] = 0;
        vy[0] = 0;
        
        top_masadami[0] = true;
    }
    
    
    public void setBalls() {
        renkli_toplar = tumtoplar - 1; //beyaz top disindakiler   
        x[1]=180; //1. topun x'i
        y[1]=200; //1. topun y'si
        x[2]=200; //2. topun x'i
        y[2]=230; //2. topun y'si
        top_masadami[1]=true; //1. top masaya konuldu
        top_masadami[2]=true; //2. top masasya konuldu
    }
    
    public void createBackGround() {
        backGround = this.createImage(this.getWidth(), this.getHeight());
        Graphics g = backGround.getGraphics();
        
        g.setColor(Color.GRAY);//arkaplan rengi
        g.fillRect(0, 0, this.getWidth(), this.getHeight());//arkaplan boyutları
        
        //kahverengi masa kenari icin rgb kodunu buldum (ismi saddlebrown)
        g.setColor(new Color(139, 69, 19));
        g.fill3DRect((int)(tableX[0]), (int)(tableY[0]), (int)(tableX[1]-tableX[0]), (int)(tableY[1]-tableY[0]), true);
        
        g.setColor(Color.GREEN);//masanın yeşil zemini
        g.fill3DRect(60, 60, 570, 220, false);
          
        g.setColor(Color.YELLOW); //alttaki score yazısı
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Score:", this.getWidth()-250, this.getHeight()-10);
    }
    
    
    public void start() {
        (new Thread(this)).start();
    }
    
    
    public void run() {
        long t1 = System.currentTimeMillis(), t2 = t1; //current time'ı millisec. cinsinden hesap
        
        while (true) {
            try {          
                t2 = System.currentTimeMillis();
                
                switch (state) {
                    case WAITING_TO_HIT_THE_BALL://updateleyip vurmasını bekle
                        calculateNextPosition(t2-t1);       
                        collisions();
                        update();
                        break;
                        
                    case MOVING: //toplar hareket ediyorsa
                        calculateNextPosition(t2-t1);   //geçen süreyi ms cinsinden hesaplar                
                        collisions();
                        update();
                        
                        boolean balls_stopped = true;
                        for (int i=0; balls_stopped && i<tumtoplar; ++i) {//3 top için 3 KERE
                            balls_stopped = (vx[i]==0) && (vy[i]==0); //balls_stopped= topun hızı 0a eşitken
                        }
                        if (balls_stopped) { //toplar durduysa
                            state = WAITING_TO_HIT_THE_BALL; //vurmasını bekleme durumuna al
                        }
                        break;
                        
                    case FINISHING:
                        setBalls();   
                        setWhiteBall();
                        state = WAITING_TO_START;
                        break;
                }
                
                render();//çizimleri boyamaları yapar 
                repaint();
                t1 = t2;
                
                Thread.sleep(10);
            } 
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    
    public void collisions() {
        wallCollision(); //top duvar collisionu
        ballsCollision(); //top top collisionu
    }
    
    
    public void calculateNextPosition(long millis) {
        double zaman = millis / 1000.0;
        
        for (int i=0; i<tumtoplar; ++i) if (top_masadami[i]){ //3 kere her top için
            nextX[i] = x[i] + vx[i] * zaman; //1. topun x'teki yeni pozisyonu=şimdiki pozisyon+ x'deki hız*zaman
            nextY[i] = y[i] + vy[i] * zaman; //1. topun y'deki yeni poz=şimdiki+y'dei hız*zaman          
          
            //hareket eden topun hızını çarparak yavaş bir şekilde azalt
            vx[i] *= 0.99;
            vy[i] *= 0.99;
            
            
            if (Math.abs(Math.hypot(vx[i], vy[i]))<2) {
                vx[i] = 0;
                vy[i] = 0;
            }
        }
    }
    
        
    public void ballsCollision() {
        //masadaki topların tek tek diğer toplarla çarpışıp çarpışmadığına bak
        //beyaz-kırmızı  ?
        //beyaz-sarı     ?
        //kırmızı-sarı   ?
        for (int random1=0; random1<tumtoplar; ++random1) if (top_masadami[random1]){
            for (int random2=random1+1; random2<tumtoplar; ++random2) if (top_masadami[random2]){
                boolean collision;
                if(collision = isBallsCollision(random1,random2)){ //eğer iki top çarpışıyorsa
            
                    //pozisyonları ayarla
                    int cont = 0;
                       nextX[random1] = (nextX[random1] + x[random1]) / 2; 
                       nextY[random1] = (nextY[random1] + y[random1]) / 2;
                        
                        nextX[random2] = (nextX[random2] + x[random2]) / 2;
                        nextY[random2] = (nextY[random2] + y[random2]) / 2;
                        
                        collision = isBallsCollision(random1, random2);
                        
                        ++cont;
                    
//                    while (cont <10 && collision){
//                        nextX[random1] = (nextX[random1] + x[random1]) / 2; 
//                        nextY[random1] = (nextY[random1] + y[random1]) / 2;
//                        
//                        nextX[random2] = (nextX[random2] + x[random2]) / 2;
//                        nextY[random2] = (nextY[random2] + y[random2]) / 2;
//                        
//                        collision = isBallsCollision(random1, random2);
//                        
//                        ++cont;
//                    }
                    
                    if (collision) { //bulunan sonraki pozisyonları şimdiki pozisyona eşitle
                        nextX[random1] = x[random1];
                        nextY[random1] = y[random1];
                        
                        nextX[random2] = x[random2];
                        nextY[random2] = y[random2];
                    }

                    // hızları ayarla
                    double dx = nextX[random2] - nextX[random1];
                    double dy = nextY[random2] - nextY[random1];
                    double dist = Math.hypot(nextX[random1]-nextX[random2], nextY[random1]-nextY[random2]);
                    
                    // cos(ang) = dx / dist
                    // sin(ang) = dy / dist;
                    // tg(ang) = dy / dx = sin(ang) / cos(ang)
                    double cos = dx/dist;
                    double sin = dy/dist;
                    
                    next_vx[random2] = vx[random2] - vx[random2] * cos * cos;
                    next_vx[random2] -= vy[random2] * cos * sin;
                    next_vx[random2] += vx[random1] * cos * cos;
                    next_vx[random2] += vy[random1] * cos * sin;       
                    
                    next_vy[random2] = vy[random2] - vy[random2] * sin * sin;
                    next_vy[random2] -= vx[random2] * cos * sin;
                    next_vy[random2] += vx[random1] * cos * sin;
                    next_vy[random2] += vy[random1] * sin * sin;
                    
                    next_vx[random1] = vx[random1] - vx[random1] * cos * cos;
                    next_vx[random1] -= vy[random1] * cos * sin;
                    next_vx[random1] += vx[random2] * cos * cos;
                    next_vx[random1] += vy[random2] * cos * sin;        
                    
                    next_vy[random1] = vy[random1] - vy[random1] * sin * sin;
                    next_vy[random1] -= vx[random1] * cos * sin;
                    next_vy[random1] += vx[random2] * cos * sin;
                    next_vy[random1] += vy[random2] * sin * sin;
                    
                    vx[random1] = next_vx[random1];
                    vy[random1] = next_vy[random1];
                    
                    vx[random2] = next_vx[random2];
                    vy[random2] = next_vy[random2];
                }
            }
        }
    }
    
    public boolean balls_distance(int ball1, int ball2) { //2 topun arasindaki mesafeyi hesaplar
        return Math.hypot(x[ball1]-x[ball2], y[ball1]-y[ball2]) < 2*ballradius;
    }
    
    public boolean isBallsCollision(int ball1, int ball2) {
        return Math.hypot(nextX[ball1]-nextX[ball2], nextY[ball1]-nextY[ball2]) < 2*ballradius;
    }
    
    public void update() {       
        for (int i=0; i<tumtoplar; ++i) if(top_masadami[i]){//masadaki 3 top için her birinin pozisyonunu yenile
            x[i] = nextX[i];
            y[i] = nextY[i];        
        }
    }
    
    
    public void wallCollision() { //top duvar collisionu
                
        for (int i=0; i<tumtoplar; ++i) if (top_masadami[i]) { //masadaki her top için
           
            if (nextX[i]-ballradius<60) { //sol duvar 
                nextX[i] =60 + ballradius; //pozisyonunu ayarlar
                vx[i] *= -1; //hızını eksi yaparak ters yöne gitmesini sağlar
            }
            else if (nextX[i]+ballradius>630){ //sağ duvar
                nextX[i] = 630-ballradius;
                vx[i] *= -1;
            }
           
            if (nextY[i]-ballradius<60) { //üst duvar
                nextY[i] = 60 + ballradius;
                vy[i] *= -1;
            } 
            else if (nextY[i]+ballradius>280) { //alt duvar
                nextY[i] = 280 - ballradius;
                vy[i] *= -1;
            }
        }
    }
     
    
    public void render() { //renkler ayarlanıyor

        Graphics gBackBuffer = backBuffer.getGraphics();
        gBackBuffer.setFont(new Font("Courier", Font.BOLD, 20));//yazi tipi,fontu,büyüklüğü
        
        // masa
        gBackBuffer.drawImage(backGround, 0, 0, null);          
        
        // top
        if (top_masadami[0]) { //0. top için (beyaz)
            gBackBuffer.setColor(Color.WHITE);
            gBackBuffer.fillOval((int)(x[0]-ballradius), (int)(y[0]-ballradius), (int)(ballradius*2), (int)(ballradius*2));
        }
        
        gBackBuffer.setColor(Color.RED);
        if (top_masadami[1]){ //1. top için (kirmizi olan)
            gBackBuffer.fillOval((int)(x[1]-ballradius), (int)(y[1]-ballradius), (int)(ballradius*2), (int)(ballradius*2)); 
        }
        
         gBackBuffer.setColor(Color.YELLOW); //3. top için (sarı)
        if (top_masadami[2]){//
            gBackBuffer.fillOval((int)(x[2]-ballradius), (int)(y[2]-ballradius), (int)(ballradius*2), (int)(ballradius*2)); 
        }
        
        // istaka
        if (state == WAITING_TO_HIT_THE_BALL) drawStick(gBackBuffer); 
        
        // baslangic mesaji
        if (state == WAITING_TO_START) {            
            int mX = this.getWidth()/2-85;
            int mY = this.getHeight()/2;
                        
            gBackBuffer.setColor(Color.BLACK);
            gBackBuffer.drawString("Click to start", mX+2, mY+2);
        }
    }
    
    
    public void drawStick(Graphics gBackBuffer) {
            
        double dist = Math.hypot(x[0]-mouseX, y[0]-mouseY);
        double dXNormalized = (mouseX-x[0])/dist;
        double dYNormalized = (mouseY-y[0])/dist;
        double strength = (clicked) ? strength()/10 : 1;
        double x1 = x[0] + dXNormalized * (ballradius+strength);
        double x2 = x[0] + dXNormalized * (ballradius+stickLength+strength);
        double y1 = y[0] + dYNormalized * (ballradius+strength);
        double y2 = y[0] + dYNormalized * (ballradius+stickLength+strength);
        
        
        // ıstaka çizimi
        gBackBuffer.setColor(Color.cyan);
        gBackBuffer.drawLine((int)x1, (int)y1, (int)x2, (int)y2);     
    }
   
    public double strength() { //ıstakaya vuruş gücü 
        if (clicked) { 
            return Math.min(MAX_STRENGTH, 10 * Math.hypot(x[0]-mouseXsonraki,y[0]-mouseYsonraki));//2 value'nun minimum olanını alır
        }                   // public final int MAX_STRENGTH = 1000; 
        else {
            return Math.min(MAX_STRENGTH, 10 * Math.hypot(mouseX-mouseXsonraki, mouseY-mouseYsonraki));
        }
    }
    
    public void paint(Graphics g) {
        g.drawImage(backBuffer, 0, 0, null);
    }
         
    // MOUSE LISTENER METHODS
    
    public void mousePressed(MouseEvent e) {
        clicked = true;
    }
    
    public void mouseReleased(MouseEvent e) {
        
        
        if (state==WAITING_TO_HIT_THE_BALL) {
            double dStickBall = Math.hypot(x[0]-mouseX, y[0]-mouseY);
            double dXNormalized = (x[0]-mouseX)/dStickBall;
            double dYNormalized = (y[0]-mouseY)/dStickBall;
            double strength = strength();
            
            if (strength>0) {
                state = MOVING;
                vx[0] = strength * dXNormalized;
                vy[0] = strength * dYNormalized;
            }
        }
        
        clicked = false;
    }
    
    public void mouseClicked(MouseEvent e) { //oyunun başındaki click to start özelliği için
        if (state == WAITING_TO_START) {     //daha oyun başlamadıysa
            state = WAITING_TO_HIT_THE_BALL; //
        }
    }
    
    public void mouseEntered(MouseEvent e) {
        // EMPTY
    }
    
    public void mouseExited(MouseEvent e) {
        // EMPTY
    }
    
    
    // MOUSEMOTIONLISTENER METHODS
    
    public void mouseMoved(MouseEvent e) { //mouse hareket edilince
        mouseXsonraki = e.getX();        //mouseun x'i ve y'si için   
        mouseYsonraki = e.getY();        //eski değerleri yeniye eşitle
        mouseX = mouseXsonraki;            
        mouseY = mouseYsonraki;
    }
    
    public void mouseDragged(MouseEvent e) {
        mouseXsonraki = e.getX();
        mouseYsonraki = e.getY();
    }     
    
    
    public static void main(String[] args) {
        new Bilardo();
    }
}
