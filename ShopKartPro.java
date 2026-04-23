import java.awt.*;
 import java.util.*;
 import java.util.List;
import javax.swing.*;
 import javax.swing.border.*;
 import javax.swing.table.*;
class OutOfStockException extends Exception { public OutOfStockException(String n){ super("'"+n+"' is out of stock or insufficient quantity."); } }
class EmptyCartException    extends Exception { public EmptyCartException(){ super("Your cart is empty. Add items before checkout."); } }

abstract class Product {
    private int id; private String name; private double price; private int stock;
    public Product(int id,String name,double price,int stock){ this.id=id; this.name=name; this.price=price; this.stock=stock; }
    public int getId(){ return id; } public String getName(){ return name; } public double getPrice(){ return price; }
    public int getStock(){ return stock; } public void setStock(int s){ stock=s; }
    public abstract String getCategory();
}
class Electronics extends Product { private String brand; public Electronics(int id,String n,double p,int s,String b){ super(id,n,p,s); brand=b; } public String getBrand(){ return brand; } public String getCategory(){ return "Electronics"; } }
class Clothing    extends Product { private String size;  public Clothing(int id,String n,double p,int s,String sz){ super(id,n,p,s); size=sz; }  public String getSize()  { return size;  } public String getCategory(){ return "Clothing";    } }
class Grocery     extends Product { private String exp;   public Grocery(int id,String n,double p,int s,String e){  super(id,n,p,s); exp=e;   }  public String getExpiry(){ return exp;   } public String getCategory(){ return "Grocery";     } }
class SportsEquipments  extends Product { private String exp;   public SportsEquipments(int id,String n,double p,int s,String e){  super(id,n,p,s); exp=e;   }  public String getExpiry(){ return exp;   } public String getCategory(){ return "Sports";     } }

class CartItem { private Product p; private int qty;
    public CartItem(Product p,int qty){ this.p=p; this.qty=qty; }
    public Product getProduct(){ return p; } public int getQty(){ return qty; } public void setQty(int q){ qty=q; }
    public double getSubtotal(){ return p.getPrice()*qty; }
}

class StockSyncThread extends Thread { private List<Product> catalog; private volatile boolean running=true;
    public StockSyncThread(List<Product> c){ catalog=c; setDaemon(true); }
    public void stopSync(){ running=false; }
    public void run(){ System.out.println("[StockSync] Background thread started.");
        while(running){ try{ Thread.sleep(15000); synchronized(catalog){ for(Product p:catalog) System.out.println("[StockSync] Checked: "+p.getName()); } } catch(InterruptedException e){ Thread.currentThread().interrupt(); } }
        System.out.println("[StockSync] Thread stopped."); }
}

class PaymentThread extends Thread { private double amount; private String mode; private Runnable onSuccess,onFail;
    public PaymentThread(double a,String m,Runnable ok,Runnable fail){ amount=a; mode=m; onSuccess=ok; onFail=fail; }
    public void run(){ try{ System.out.println("[Payment] Processing "+mode+" Rs."+amount+"..."); Thread.sleep(2500); onSuccess.run(); } catch(InterruptedException e){ Thread.currentThread().interrupt(); onFail.run(); } }
}

class ShoppingCartEngine { private List<CartItem> items=new ArrayList<>(); private List<Product> catalog;
    public ShoppingCartEngine(List<Product> c){ catalog=c; }
    public synchronized void addItem(Product p,int qty) throws OutOfStockException {
        if(p.getStock()<qty) throw new OutOfStockException(p.getName());
        for(CartItem ci:items){ if(ci.getProduct().getId()==p.getId()){ int nq=ci.getQty()+qty; if(p.getStock()<nq) throw new OutOfStockException(p.getName()); ci.setQty(nq); return; } }
        items.add(new CartItem(p,qty)); }
    public synchronized boolean removeItem(int id){ Iterator<CartItem> it=items.iterator(); while(it.hasNext()){ if(it.next().getProduct().getId()==id){ it.remove(); return true; } } return false; }
    public synchronized void updateQty(int id,int qty) throws OutOfStockException { for(CartItem ci:items){ if(ci.getProduct().getId()==id){ if(ci.getProduct().getStock()<qty) throw new OutOfStockException(ci.getProduct().getName()); ci.setQty(qty); return; } } }
    public synchronized List<CartItem> getItems(){ return Collections.unmodifiableList(items); }
    public synchronized double getTotal(){ double t=0; for(CartItem c:items) t+=c.getSubtotal(); return t; }
    public synchronized int getTotalItems(){ int n=0; for(CartItem c:items) n+=c.getQty(); return n; }
    public synchronized void checkout() throws EmptyCartException { if(items.isEmpty()) throw new EmptyCartException(); for(CartItem ci:items) ci.getProduct().setStock(ci.getProduct().getStock()-ci.getQty()); items.clear(); }
    public synchronized void clear(){ items.clear(); }
}

public class ShopKartPro extends JFrame {
    static final Color BG_DARK=new Color(15,15,30),BG_PANEL=new Color(25,25,50),BG_CARD=new Color(35,35,65);
    static final Color ACCENT=new Color(80,160,255),ACCENT2=new Color(255,100,80),TXT_W=new Color(230,235,255);
    static final Color TXT_DIM=new Color(140,145,170),GREEN=new Color(60,200,120),YELLOW=new Color(255,200,60);

    List<Product> catalog=new ArrayList<>(); ShoppingCartEngine cart; StockSyncThread syncThread; boolean isOnline=true;
    JLabel modeLabel,totalLabel,itemCountLabel,statusBar; JToggleButton modeToggle;
    JTable catalogTable,cartTable; DefaultTableModel catalogModel,cartModel;
    JTextField qtyField; JComboBox<String> categoryFilter; JTextArea logArea;

    public ShopKartPro() {
        catalog.add(new Electronics(1,"Samsung 65\" QLED TV",85000,10,"Samsung")); catalog.add(new Electronics(2,"Apple iPhone 15",79999,15,"Apple"));
        catalog.add(new Electronics(3,"Sony WH-1000XM5 Headset",29990,20,"Sony")); catalog.add(new Electronics(4,"Dell Inspiron Laptop",55000,8,"Dell"));
        catalog.add(new Electronics(5,"boAt Rockerz 450",1799,50,"boAt"));
        catalog.add(new Clothing(6,"Levi's 511 Slim Jeans",3499,30,"32")); catalog.add(new Clothing(7,"Nike Dri-FIT T-Shirt",1299,40,"L"));
        catalog.add(new Clothing(8,"Zara Floral Dress",2999,25,"M")); catalog.add(new Clothing(9,"Puma Sports Jacket",4599,18,"XL")); 
catalog.add(new Clothing(10,"H&M Cotton Hoodie",1899,35,"S"));
 // Electronics
catalog.add(new Electronics(11, "Apple iPhone 16", 89999, 20, "Apple"));
catalog.add(new Electronics(12, "Apple iPhone 17", 99999, 15, "Apple"));
catalog.add(new Electronics(13, "Apple iPhone 17 Pro", 119999, 10, "Apple"));
catalog.add(new Electronics(14, "Wireless Headphones", 2999, 40, "Boat"));

catalog.add(new Clothing(15, "Silk Saree", 4999, 15, "Free"));
catalog.add(new Clothing(16, "Cotton Saree", 1999, 25, "Free"));
catalog.add(new Clothing(17, "Banarasi Saree", 6999, 10, "Free"));
catalog.add(new Clothing(18, "Chiffon Saree", 2999, 20, "Free"));
catalog.add(new Clothing(19, "Georgette Saree", 3499, 18, "Free"));
catalog.add(new Clothing(20, "Anarkali Kurti", 2499, 20, "M"));
catalog.add(new Clothing(21, "Straight Cut Kurti", 1499, 30, "L"));
catalog.add(new Clothing(22, "A-Line Kurti", 1799, 25, "S"));
catalog.add(new Clothing(23, "Printed Kurti", 999, 40, "M"));
catalog.add(new Clothing(24, "Embroidered Kurti", 1999, 15, "L"));
catalog.add(new Clothing(25, "Nike Running Shoes", 5999, 20, "42"));
catalog.add(new Clothing(26, "Adidas Sneakers", 5499, 18, "41"));
catalog.add(new Clothing(27, "Puma Sports Shoes", 4999, 22, "43"));
catalog.add(new Clothing(28, "Reebok Training Shoes", 4599, 16, "42"));
catalog.add(new Clothing(29, "Skechers Casual Shoes", 6499, 14, "44"));
catalog.add(new SportsEquipments(30, "Yonex Badminton Racket", 2499, 20, "Yonex"));
catalog.add(new SportsEquipments(31, "Cosco Football", 799, 30, "Cosco"));
catalog.add(new SportsEquipments(32, "SG Cricket Bat", 2999, 15, "SG"));
catalog.add(new SportsEquipments(33, "Nivia Volleyball", 699, 25, "Nivia"));
catalog.add(new SportsEquipments(34, "Table Tennis Racket Set", 999, 18, "GKI"));
catalog.add(new SportsEquipments(35, "Skipping Rope", 199, 50, "Generic"));
catalog.add(new SportsEquipments(36, "Dumbbells 5kg Pair", 1499, 12, "Kore"));

// Grocery
catalog.add(new Grocery(37, "Oreo Biscuits 1kg", 250, 60, "2026-06-01"));
catalog.add(new Grocery(38, "Mango Ice Cream 1kg", 350, 30, "2026-05-10"));
        catalog.add(new Grocery(39,"Amul Butter 500g",285,100,"2025-09-01")); catalog.add(new Grocery(40,"Tata Salt 1kg",28,200,"2026-12-01"));
        catalog.add(new Grocery(41,"Maggi Noodles (12-pack)",180,80,"2025-11-01"));
 catalog.add(new Grocery(42,"Fortune Sunflower Oil 5L",720,60,"2026-03-01")); catalog.add(new Grocery(43,"Parle-G Biscuits 1kg",120,150,"2025-10-01"));
        cart=new ShoppingCartEngine(catalog); syncThread=new StockSyncThread(catalog); syncThread.start();
        setTitle("ShopKart Pro  |  Online Mode"); setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(1060,720); setMinimumSize(new Dimension(900,620)); setLocationRelativeTo(null); getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout()); add(buildHeader(),BorderLayout.NORTH); add(buildCenter(),BorderLayout.CENTER); add(buildStatusBar(),BorderLayout.SOUTH); setVisible(true);
    }

    JPanel buildHeader() {
        JPanel h=new JPanel(new BorderLayout()); h.setBackground(BG_PANEL); h.setBorder(BorderFactory.createMatteBorder(0,0,2,0,ACCENT));
        JLabel logo=new JLabel("  \uD83D\uDED2  ShopKart Pro"); logo.setFont(new Font("Segoe UI",Font.BOLD,22)); logo.setForeground(TXT_W); logo.setBorder(new EmptyBorder(12,16,12,0));
        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,12,8)); right.setBackground(BG_PANEL);
        modeLabel=new JLabel("\u25CF ONLINE"); modeLabel.setFont(new Font("Segoe UI",Font.BOLD,13)); modeLabel.setForeground(GREEN);
        modeToggle=new JToggleButton("Switch to Offline"); styleBtn(modeToggle,ACCENT,BG_CARD); modeToggle.addActionListener(e->toggleMode());
        itemCountLabel=new JLabel("Cart: 0 items"); itemCountLabel.setFont(new Font("Segoe UI",Font.PLAIN,13)); itemCountLabel.setForeground(TXT_DIM);
        right.add(itemCountLabel); right.add(modeLabel); right.add(modeToggle);
        h.add(logo,BorderLayout.WEST); h.add(right,BorderLayout.EAST); return h;
    }

    JSplitPane buildCenter() { JSplitPane sp=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,buildLeft(),buildRight()); sp.setDividerLocation(580); sp.setDividerSize(4); sp.setBackground(BG_DARK); sp.setBorder(null); return sp; }

    JPanel buildLeft() {
        JPanel p=new JPanel(new BorderLayout(0,8)); p.setBackground(BG_DARK); p.setBorder(new EmptyBorder(10,10,10,5));
        JPanel fb=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); fb.setBackground(BG_DARK);
        categoryFilter=new JComboBox<>(new String[]{"All","Electronics","Clothing","Grocery","Sports"}); styleCombo(categoryFilter);
        categoryFilter.addActionListener(e->refreshCatalogTable((String)categoryFilter.getSelectedItem()));
        qtyField=new JTextField("1",3); styleTF(qtyField);
        JButton addBtn=new JButton("Add to Cart  +"); styleBtn(addBtn,GREEN,BG_CARD); addBtn.addActionListener(e->addToCart());
        fb.add(label("Category:")); fb.add(categoryFilter); fb.add(label("  Qty:")); fb.add(qtyField); fb.add(addBtn);
        catalogModel=new DefaultTableModel(new String[]{"ID","Product Name","Category","Price (Rs.)","Stock"},0){ public boolean isCellEditable(int r,int c){ return false; } };
        catalogTable=new JTable(catalogModel); styleTable(catalogTable);
        int[] cw={35,185,90,90,55}; for(int i=0;i<cw.length;i++) catalogTable.getColumnModel().getColumn(i).setPreferredWidth(cw[i]);
        refreshCatalogTable(null); JScrollPane sc=new JScrollPane(catalogTable); styleSP(sc);
        p.add(secTitle("\uD83D\uDCE6  Product Catalog"),BorderLayout.NORTH); p.add(sc,BorderLayout.CENTER); p.add(fb,BorderLayout.SOUTH); return p;
    }

    JPanel buildRight() {
        JPanel p=new JPanel(new BorderLayout(0,8)); p.setBackground(BG_DARK); p.setBorder(new EmptyBorder(10,5,10,10));
        cartModel=new DefaultTableModel(new String[]{"Product","Qty","Price","Subtotal"},0){ public boolean isCellEditable(int r,int c){ return false; } };
        cartTable=new JTable(cartModel); styleTable(cartTable); cartTable.getColumnModel().getColumn(0).setPreferredWidth(160);
        JScrollPane csc=new JScrollPane(cartTable); csc.setPreferredSize(new Dimension(400,220)); styleSP(csc);
        totalLabel=new JLabel("Total:  Rs. 0.00"); totalLabel.setFont(new Font("Segoe UI",Font.BOLD,16)); totalLabel.setForeground(YELLOW); totalLabel.setBorder(new EmptyBorder(4,4,4,4));
        JPanel btnRow=new JPanel(new GridLayout(1,3,6,0)); btnRow.setBackground(BG_DARK);
        JButton remBtn=new JButton("Remove Item"),clrBtn=new JButton("Clear Cart"),chkBtn=new JButton("\u2714  Checkout");
        styleBtn(remBtn,ACCENT2,BG_CARD); styleBtn(clrBtn,new Color(120,70,70),BG_CARD); styleBtn(chkBtn,GREEN,BG_CARD);
        remBtn.addActionListener(e->removeFromCart()); clrBtn.addActionListener(e->clearCart()); chkBtn.addActionListener(e->checkout());
        btnRow.add(remBtn); btnRow.add(clrBtn); btnRow.add(chkBtn);
        JPanel botCart=new JPanel(new BorderLayout(0,6)); botCart.setBackground(BG_DARK); botCart.add(totalLabel,BorderLayout.NORTH); botCart.add(btnRow,BorderLayout.SOUTH);
        logArea=new JTextArea(6,30); logArea.setEditable(false); logArea.setBackground(new Color(10,10,22)); logArea.setForeground(new Color(100,220,120));
        logArea.setFont(new Font("Consolas",Font.PLAIN,11)); logArea.setBorder(new EmptyBorder(6,8,6,8));
        JScrollPane lsc=new JScrollPane(logArea); lsc.setBorder(BorderFactory.createLineBorder(new Color(40,40,80),1));
        JPanel topR=new JPanel(new BorderLayout(0,4)); topR.setBackground(BG_DARK); topR.add(secTitle("\uD83D\uDED2  Your Cart"),BorderLayout.NORTH); topR.add(csc,BorderLayout.CENTER); topR.add(botCart,BorderLayout.SOUTH);
        JPanel logWrap=new JPanel(new BorderLayout(0,4)); logWrap.setBackground(BG_DARK); logWrap.add(secTitle("\uD83D\uDCCB  Activity Log"),BorderLayout.NORTH); logWrap.add(lsc,BorderLayout.CENTER);
        p.add(topR,BorderLayout.CENTER); p.add(logWrap,BorderLayout.SOUTH); return p;
    }

    JPanel buildStatusBar() { JPanel bar=new JPanel(new BorderLayout()); bar.setBackground(new Color(10,10,22)); bar.setBorder(new EmptyBorder(4,12,4,12)); statusBar=new JLabel("Ready  |  ShopKart Pro v1.0  |  Online Mode Active"); statusBar.setFont(new Font("Consolas",Font.PLAIN,11)); statusBar.setForeground(TXT_DIM); bar.add(statusBar,BorderLayout.WEST); return bar; }

    void toggleMode() {
        isOnline=!isOnline;
        if(isOnline){ modeLabel.setText("\u25CF ONLINE"); modeLabel.setForeground(GREEN); modeToggle.setText("Switch to Offline"); setTitle("ShopKart Pro  |  Online Mode"); statusBar.setText("Mode: ONLINE – Real-time sync active"); log("[MODE] Switched to ONLINE mode."); if(!syncThread.isAlive()){ syncThread=new StockSyncThread(catalog); syncThread.start(); } }
        else         { modeLabel.setText("\u25CB OFFLINE"); modeLabel.setForeground(ACCENT2); modeToggle.setText("Switch to Online"); setTitle("ShopKart Pro  |  Offline Mode"); statusBar.setText("Mode: OFFLINE – Using local cached data"); log("[MODE] Switched to OFFLINE mode. Sync paused."); syncThread.stopSync(); }
        refreshCatalogTable((String)categoryFilter.getSelectedItem());
    }

    void addToCart() {
        int row=catalogTable.getSelectedRow(); if(row<0){ showErr("Please select a product from the catalog."); return; }
        if(!isOnline){ int c=JOptionPane.showConfirmDialog(this,"You are OFFLINE. Using cached data. Proceed?","Offline Mode",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE); if(c!=JOptionPane.YES_OPTION) return; }
        int qty; try{ qty=Integer.parseInt(qtyField.getText().trim()); if(qty<=0) throw new NumberFormatException(); } catch(NumberFormatException ex){ showErr("Enter a valid quantity (positive integer)."); return; }
        int pid=(int)catalogModel.getValueAt(row,0); Product p=findProduct(pid); if(p==null) return;
        try{ cart.addItem(p,qty); refreshCartTable(); refreshCatalogTable((String)categoryFilter.getSelectedItem()); log("[CART] Added "+qty+"x '"+p.getName()+"'  Rs."+String.format("%.2f",p.getPrice()*qty)); statusBar.setText("Added: "+p.getName()+"  (Qty: "+qty+")"); }
        catch(OutOfStockException ex){ showErr(ex.getMessage()); log("[ERROR] "+ex.getMessage()); }
    }

    void removeFromCart() {
        int row=cartTable.getSelectedRow(); if(row<0){ showErr("Select a cart item to remove."); return; }
        String name=(String)cartModel.getValueAt(row,0);
        for(CartItem ci:cart.getItems()){ if(ci.getProduct().getName().equals(name)){ cart.removeItem(ci.getProduct().getId()); break; } }
        refreshCartTable(); refreshCatalogTable((String)categoryFilter.getSelectedItem()); log("[CART] Removed '"+name+"'."); statusBar.setText("Removed: "+name);
    }

    void clearCart() { if(cart.getItems().isEmpty()){ showErr("Cart is already empty."); return; } int c=JOptionPane.showConfirmDialog(this,"Clear all cart items?","Clear Cart",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE); if(c==JOptionPane.YES_OPTION){ cart.clear(); refreshCartTable(); log("[CART] Cart cleared."); statusBar.setText("Cart cleared."); } }

    void checkout() {
        try{
            if(cart.getItems().isEmpty()) throw new EmptyCartException();
            double total=cart.getTotal();
            String[] opts=isOnline?new String[]{"UPI","Credit Card","Debit Card","Net Banking","Cancel"}:new String[]{"Cash on Delivery","Cancel"};
            int choice=JOptionPane.showOptionDialog(this,String.format("Order Total:  Rs. %.2f\n\nSelect Payment Method:",total),"Checkout",JOptionPane.DEFAULT_OPTION,JOptionPane.QUESTION_MESSAGE,null,opts,opts[0]);
            if(choice<0||opts[choice].equals("Cancel")) return;
            String payMode=opts[choice];
            if(isOnline){
                JDialog dlg=buildProcessingDlg(); dlg.setVisible(true);
                new PaymentThread(total,payMode,
                    ()->SwingUtilities.invokeLater(()->{ dlg.dispose(); try{ cart.checkout(); }catch(EmptyCartException ignored){} refreshCartTable(); refreshCatalogTable((String)categoryFilter.getSelectedItem()); log(String.format("[ORDER] \u2714 Online %s  Rs.%.2f  SUCCESS",payMode,total)); showOk(String.format("Payment Successful!\n\nMethod: %s\nAmount: Rs. %.2f\n\nThank you for shopping at ShopKart Pro!",payMode,total)); statusBar.setText("Payment successful via "+payMode); }),
                    ()->SwingUtilities.invokeLater(()->{ dlg.dispose(); log("[ORDER] \u2716 Payment failed."); showErr("Payment failed. Please try again."); })
                ).start();
            } else { cart.checkout(); refreshCartTable(); refreshCatalogTable((String)categoryFilter.getSelectedItem()); log(String.format("[ORDER] \u2714 Offline COD  Rs.%.2f",total)); showOk(String.format("Order Placed (COD)!\n\nAmount: Rs. %.2f\n\nYour order will be delivered soon.",total)); statusBar.setText("Offline COD order placed."); }
        } catch(EmptyCartException ex){ showErr(ex.getMessage()); log("[ERROR] "+ex.getMessage()); }
    }

    void refreshCatalogTable(String f){ catalogModel.setRowCount(0); for(Product p:catalog){ if(f==null||f.equals("All")||p.getCategory().equals(f)) catalogModel.addRow(new Object[]{p.getId(),p.getName(),p.getCategory(),String.format("%.2f",p.getPrice()),p.getStock()}); } }
    void refreshCartTable(){ cartModel.setRowCount(0); for(CartItem ci:cart.getItems()) cartModel.addRow(new Object[]{ci.getProduct().getName(),ci.getQty(),String.format("%.2f",ci.getProduct().getPrice()),String.format("%.2f",ci.getSubtotal())}); totalLabel.setText("Total:  Rs. "+String.format("%.2f",cart.getTotal())); itemCountLabel.setText("Cart: "+cart.getTotalItems()+" item(s)"); }

    void styleTable(JTable t){ t.setBackground(BG_CARD); t.setForeground(TXT_W); t.setGridColor(new Color(50,50,80)); t.setSelectionBackground(new Color(50,100,200,180)); t.setSelectionForeground(Color.WHITE); t.setRowHeight(26); t.setFont(new Font("Segoe UI",Font.PLAIN,12)); t.setShowHorizontalLines(true); t.setShowVerticalLines(false); JTableHeader h=t.getTableHeader(); h.setBackground(BG_PANEL); h.setForeground(ACCENT); h.setFont(new Font("Segoe UI",Font.BOLD,12)); h.setBorder(BorderFactory.createMatteBorder(0,0,2,0,ACCENT)); h.setReorderingAllowed(false); }
    void styleSP(JScrollPane s){ s.setBorder(BorderFactory.createLineBorder(new Color(40,40,80),1)); s.getViewport().setBackground(BG_CARD); }
    void styleBtn(AbstractButton b,Color fg,Color bg){ b.setBackground(bg); b.setForeground(fg); b.setFont(new Font("Segoe UI",Font.BOLD,12)); b.setFocusPainted(false); b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(fg,1),new EmptyBorder(5,10,5,10))); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
    void styleTF(JTextField f){ f.setBackground(BG_CARD); f.setForeground(TXT_W); f.setCaretColor(TXT_W); f.setFont(new Font("Segoe UI",Font.PLAIN,12)); f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ACCENT,1),new EmptyBorder(3,6,3,6))); }
    void styleCombo(JComboBox<String> c){ c.setBackground(BG_CARD); c.setForeground(TXT_W); c.setFont(new Font("Segoe UI",Font.PLAIN,12)); }
    JLabel label(String t){ JLabel l=new JLabel(t); l.setForeground(TXT_DIM); l.setFont(new Font("Segoe UI",Font.PLAIN,12)); return l; }
    JLabel secTitle(String t){ JLabel l=new JLabel(t); l.setFont(new Font("Segoe UI",Font.BOLD,14)); l.setForeground(ACCENT); l.setBorder(new EmptyBorder(0,0,4,0)); return l; }
    JDialog buildProcessingDlg(){ JDialog d=new JDialog(this,"Processing Payment",true); d.setSize(300,110); d.setLocationRelativeTo(this); d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); d.getContentPane().setBackground(BG_PANEL); JLabel lbl=new JLabel("\u23F3  Processing your payment...",SwingConstants.CENTER); lbl.setForeground(TXT_W); lbl.setFont(new Font("Segoe UI",Font.BOLD,14)); d.add(lbl); new Thread(()->{ try{ Thread.sleep(3000); }catch(InterruptedException ignored){} SwingUtilities.invokeLater(d::dispose); }).start(); return d; }

    void showErr(String m){ JOptionPane.showMessageDialog(this,m,"Error",JOptionPane.ERROR_MESSAGE); }
    void showOk(String m) { JOptionPane.showMessageDialog(this,m,"Success \u2714",JOptionPane.INFORMATION_MESSAGE); }
    void log(String m){ logArea.append(m+"\n"); logArea.setCaretPosition(logArea.getDocument().getLength()); }
    Product findProduct(int id){ for(Product p:catalog) if(p.getId()==id) return p; return null; }

    public static void main(String[] args){ try{ UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }catch(Exception ignored){} SwingUtilities.invokeLater(ShopKartPro::new); }
}