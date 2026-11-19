package com.monobogdan.simpletelegram;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.monobogdan.simpletelegram.transport.ClientManager;
import org.json.JSONArray;
import org.json.JSONObject;

/** R1K1 trading, gifts and crafting client. All balances and ownership decisions remain server-side. */
public class TradingActivity extends Activity {
    private LinearLayout content, tabs;
    private TextView status;
    private String active = "Маркет";
    private static final int RED = 0xFFFF2341;

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private TextView label(String s, int size, int color) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setPadding(dp(5),dp(5),dp(5),dp(5)); return t;
    }
    private JSONObject obj(Object... pairs) {
        JSONObject o = new JSONObject();
        try { for (int i=0;i+1<pairs.length;i+=2) o.put(String.valueOf(pairs[i]), pairs[i+1]); } catch(Exception ignored) {}
        return o;
    }
    private void message(final String title, final String value) {
        runOnUiThread(new Runnable(){ @Override public void run(){
            status.setText(title + ": ответ сервера получен");
            new AlertDialog.Builder(TradingActivity.this).setTitle(title).setMessage(value).setPositiveButton("OK",null).show();
        }});
    }
    private void get(final String path, final String title, final boolean cards, final String action) {
        status.setText("Загрузка: " + path);
        ClientManager.getCurrent().apiGet(path, new ClientManager.Response(){ @Override public void onReady(final String response){
            runOnUiThread(new Runnable(){ @Override public void run(){
                if(cards) renderCards(response, action); else message(title,response);
            }});
        }});
    }
    private void post(final String path, JSONObject body, final String title) {
        ClientManager.getCurrent().apiPost(path, body.toString(), new ClientManager.Response(){ @Override public void onReady(String response){ message(title,response); }});
    }
    private JSONArray extractArray(String raw) throws Exception {
        Object parsed = new org.json.JSONTokener(raw).nextValue();
        if(parsed instanceof JSONArray) return (JSONArray)parsed;
        if(parsed instanceof JSONObject) {
            JSONObject o=(JSONObject)parsed;
            String[] keys={"items","listings","gifts","recipes","data","results"};
            for(String k:keys) if(o.opt(k) instanceof JSONArray) return o.getJSONArray(k);
        }
        return new JSONArray();
    }
    private void renderCards(String raw, final String action) {
        content.removeAllViews();
        try {
            JSONArray a=extractArray(raw);
            if(a.length()==0) content.addView(label("Сервер вернул пустой список. Проверь формат JSON и наличие предметов.",14,0xFFCCCCCC));
            for(int i=0;i<a.length();i++) {
                final JSONObject item=a.optJSONObject(i); if(item==null) continue;
                LinearLayout card=(LinearLayout)getLayoutInflater().inflate(R.layout.item_market_card,content,false);
                String name=item.optString("name",item.optString("title",item.optString("gift_name","Без названия")));
                String rarity=item.optString("rarity","Без редкости");
                String owner=item.optString("owner_username",item.optString("owner",""));
                String price=item.optString("price","—"); String currency=item.optString("currency","Bytes");
                ((TextView)card.findViewById(R.id.item_title)).setText(name);
                final android.widget.ImageView picture = card.findViewById(R.id.item_image);
                String image = item.optString("image_url", item.optString("image", ""));
                if (image.length() > 0 && PhotoCache.getInstance() != null) {
                    final String imageUrl = image.startsWith("http://") || image.startsWith("https://") ? image : ClientManager.getCurrent().getPhotoUrl(image);
                    PhotoCache.getInstance().scheduleDownload(imageUrl, new PhotoCache.Response() {
                        @Override public void onReady(final android.graphics.Bitmap bitmap) { runOnUiThread(new Runnable(){@Override public void run(){picture.setImageBitmap(bitmap);}}); }
                        @Override public void onFailed() { }
                    });
                }
                ((TextView)card.findViewById(R.id.item_meta)).setText((rarity.length()>0?rarity:"—")+(owner.length()>0?"  •  @"+owner:""));
                ((TextView)card.findViewById(R.id.item_price)).setText("Цена: "+price+" "+currency);
                String type=item.optString("listing_type", "fixed");
                TextView auction=card.findViewById(R.id.item_auction);
                if("auction".equalsIgnoreCase(type)) auction.setText("АУКЦИОН • текущая ставка: "+item.optString("current_bid","—")+" "+currency+" • осталось: "+item.optString("time_remaining",item.optString("ends_at","—")));
                else auction.setText("");
                Button b=card.findViewById(R.id.item_action);
                final String id=item.optString("listing_id",item.optString("id",item.optString("gift_id")));
                if("sell".equals(action)) { b.setText("Ваша продажа"); b.setEnabled(false); }
                else if("gift".equals(action)) { b.setText("Отправить"); b.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){showSendGift(item);}}); }
                else if("craft".equals(action)) { b.setText("Скрафтить"); b.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){post("/craft",obj("recipe_id",item.optString("recipe_id",id)),"Крафт");}}); }
                else if("auction".equalsIgnoreCase(type)) { b.setText("Ставки скоро"); b.setEnabled(false); }
                else { b.setText("Купить"); b.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){post("/market/buy",obj("listing_id",id),"Покупка");}}); }
                content.addView(card);
            }
            status.setText("Загружено карточек: "+a.length()+" • источник: Node.js API");
        } catch(Exception e) { content.addView(label("Ошибка JSON: "+e.getMessage()+"\nОтвет: "+raw,13,0xFFFF9AA8)); }
    }
    private void promptSell() {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(12),0,dp(12),0);
        final EditText id=new EditText(this); id.setHint("ID NFT или подарка"); box.addView(id);
        final EditText price=new EditText(this); price.setHint("Цена"); price.setInputType(2); box.addView(price);
        final EditText currency=new EditText(this); currency.setHint("Валюта: Bytes или Coins"); currency.setText("Bytes"); box.addView(currency);
        final EditText kind=new EditText(this); kind.setHint("Тип: nft или gift"); kind.setText("nft"); box.addView(kind);
        final EditText mode=new EditText(this); mode.setHint("Тип продажи: fixed или auction"); mode.setText("fixed"); box.addView(mode);
        new AlertDialog.Builder(this).setTitle("Выставить предмет").setView(box).setNegativeButton("Отмена",null).setPositiveButton("Выставить",new DialogInterface.OnClickListener(){@Override public void onClick(DialogInterface d,int w){
            String c=currency.getText().toString().trim(); if(!"Bytes".equalsIgnoreCase(c)&&!"Coins".equalsIgnoreCase(c)){Toast.makeText(TradingActivity.this,"Валюта должна быть Bytes или Coins",Toast.LENGTH_LONG).show();return;}
            try { long p=Long.parseLong(price.getText().toString().trim()); if(p<=0) throw new Exception();
                JSONObject payload=obj("price",p,"currency",c,"listing_type",mode.getText().toString().trim());
                String itemType=kind.getText().toString().trim();
                payload.put("item_type",itemType);
                payload.put("nft".equalsIgnoreCase(itemType)?"nft_id":"gift_id",id.getText().toString().trim());
                post("/market/sell",payload,"Выставление на продажу"); }
            catch(Exception ex){Toast.makeText(TradingActivity.this,"Укажи корректную положительную цену",Toast.LENGTH_LONG).show();}
        }}).show();
    }
    private void showSendGift(final JSONObject item) {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(12),0,dp(12),0);
        final EditText user=new EditText(this); user.setHint("ID получателя"); user.setInputType(2); box.addView(user);
        new AlertDialog.Builder(this).setTitle("Отправить: "+item.optString("name",item.optString("title","подарок"))).setView(box).setNegativeButton("Отмена",null).setPositiveButton("Отправить",new DialogInterface.OnClickListener(){@Override public void onClick(DialogInterface d,int w){
            post("/gifts/send",obj("user_id",user.getText().toString().trim(),"gift_id",item.optString("gift_id",item.optString("id"))),"Отправка подарка");
        }}).show();
    }
    private void tab(String name, final String path, final String action) {
        Button b=new Button(this); b.setText(name); b.setTextSize(12); b.setAllCaps(false); b.setTextColor(name.equals(active)?RED:0xFFFFFFFF);
        b.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){active=name; refreshTabs(); if("sell_form".equals(action)){promptSell();return;} get(path,name,true,action);}}); tabs.addView(b);
    }
    private void refreshTabs(){ tabs.removeAllViews(); tab("Маркет","/market/list","buy"); tab("Подарки","/gifts?type=standard","gift"); tab("Архив","/gifts?type=archive","gift"); tab("Мои продажи","/market/my-listings","sell"); tab("Выставить","","sell_form"); tab("Крафт","/craft/recipes","craft"); }
    @Override protected void onCreate(Bundle state){ super.onCreate(state); setContentView(R.layout.activity_trading);
        content=findViewById(R.id.trading_content); tabs=findViewById(R.id.trading_tabs); status=findViewById(R.id.trading_status);
        findViewById(R.id.trading_back).setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){finish();}});
        refreshTabs();
        if (getIntent().getBooleanExtra("open_sell", false)) { active="Выставить"; refreshTabs(); promptSell(); }
        else get("/market/list","Маркет",true,"buy");
    }
}
