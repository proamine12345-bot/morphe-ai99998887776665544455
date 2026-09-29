package com.proamine.morpheai;

import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    static final int PICK_APK=41;
    LinearLayout root, content;
    TextView status, report, apkName, apkMeta;
    EditText prompt;
    Uri apkUri; File apkFile;

    int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    TextView text(String s,float size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.RIGHT);t.setTextDirection(View.TEXT_DIRECTION_RTL);t.setPadding(dp(16),dp(8),dp(16),dp(8));return t;}
    TextView label(String s){return text(s,13,Color.rgb(160,170,190));}
    GradientDrawable bg(int color,float radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    Button action(String s,int color){Button b=new Button(this);b.setText(s);b.setTextSize(15);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setGravity(Gravity.CENTER);b.setPadding(dp(12),0,dp(12),0);b.setBackground(bg(color,16));return b;}
    LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(12),dp(14),dp(12));c.setBackground(bg(Color.rgb(28,34,47),20));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(12));c.setLayoutParams(p);return c;}
    void addGap(int h){Space s=new Space(this);root.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(11,15,24));getWindow().setNavigationBarColor(Color.rgb(11,15,24));build();}

    void build(){
        ScrollView sv=new ScrollView(this);sv.setFillViewport(true);sv.setBackgroundColor(Color.rgb(11,15,24));
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(14),dp(16),dp(24));root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        sv.addView(root);setContentView(sv);

        LinearLayout hero=card();hero.setBackground(bg(Color.rgb(25,31,46),24));
        TextView title=text("مختبر تعديل APK بالذكاء الاصطناعي",25,Color.WHITE);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);hero.addView(title);
        TextView sub=text("تحليل • تعديل • إعادة بناء • إخراج APK",14,Color.rgb(176,190,215));hero.addView(sub);
        TextView badge=text("يدعم Gemini و OpenAI و OpenRouter",12,Color.rgb(125,210,170));hero.addView(badge);
        root.addView(hero);

        LinearLayout settingsCard=card();
        settingsCard.addView(label("الإعدادات"));
        Button settings=action("⚙  إعدادات الذكاء الاصطناعي",Color.rgb(55,72,105));settingsCard.addView(settings,new LinearLayout.LayoutParams(-1,dp(52)));settings.setOnClickListener(v->showAiSettings());
        root.addView(settingsCard);

        LinearLayout apkCard=card();
        apkCard.addView(text("📦 ملف APK",18,Color.WHITE));
        apkName=text("لم يتم اختيار أي APK",15,Color.WHITE);apkCard.addView(apkName);
        apkMeta=label("اختر تطبيقاً من هاتفك للبدء");apkCard.addView(apkMeta);
        Button pick=action("اختيار APK من الهاتف",Color.rgb(73,91,130));apkCard.addView(pick,new LinearLayout.LayoutParams(-1,dp(52)));pick.setOnClickListener(v->pickApk());
        status=text("● جاهز للبدء",13,Color.rgb(125,210,170));apkCard.addView(status);
        root.addView(apkCard);

        LinearLayout requestCard=card();
        requestCard.addView(text("✦ ماذا تريد أن أعدّل؟",18,Color.WHITE));
        requestCard.addView(label("اكتب طلبك بالعربية، وسيحلله الذكاء الاصطناعي قبل التنفيذ."));
        prompt=new EditText(this);prompt.setTextColor(Color.WHITE);prompt.setHintTextColor(Color.rgb(125,135,155));prompt.setHint("مثال: غيّر جميع واجهات التطبيق إلى تصميم احترافي واجعل اللغة عربية بالكامل");prompt.setTextSize(15);prompt.setGravity(Gravity.TOP|Gravity.RIGHT);prompt.setTextDirection(View.TEXT_DIRECTION_RTL);prompt.setPadding(dp(14),dp(14),dp(14),dp(14));prompt.setMinLines(5);prompt.setBackground(bg(Color.rgb(18,23,33),16));requestCard.addView(prompt,new LinearLayout.LayoutParams(-1,dp(150)));
        Button analyze=action("🔍 تحليل APK",Color.rgb(58,86,118));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(52));ap.setMargins(0,dp(12),0,0);requestCard.addView(analyze,ap);analyze.setOnClickListener(v->analyze());
        Button run=action("⚡ إرسال الطلب إلى الذكاء الاصطناعي",Color.rgb(92,72,150));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(56));rp.setMargins(0,dp(8),0,0);requestCard.addView(run,rp);run.setOnClickListener(v->runAi());
        root.addView(requestCard);

        LinearLayout resultCard=card();
        resultCard.addView(text("📋 النتيجة والسجل",18,Color.WHITE));
        report=text("سيظهر هنا تحليل APK ورد الذكاء الاصطناعي.",13,Color.rgb(205,210,220));report.setBackground(bg(Color.rgb(18,23,33),16));resultCard.addView(report,new LinearLayout.LayoutParams(-1,-2));
        root.addView(resultCard);

        TextView foot=text("ملاحظة: حفظ مفاتيح API يتم محلياً داخل التطبيق.",11,Color.rgb(125,135,155));root.addView(foot);
    }

    void showAiSettings(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(8),dp(4),dp(8),dp(4));box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        SharedPreferences p=getSharedPreferences("ai_settings",MODE_PRIVATE);
        String[] providers={"Gemini","OpenAI","OpenRouter"};
        Spinner provider=new Spinner(this);provider.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,providers));
        box.addView(text("مزود الذكاء الاصطناعي",15,Color.DKGRAY));box.addView(provider);
        EditText name=new EditText(this);name.setHint("اسم المفتاح");name.setTextDirection(View.TEXT_DIRECTION_RTL);box.addView(name);
        EditText key=new EditText(this);key.setHint("مفتاح API");key.setInputType(129);box.addView(key);
        EditText model=new EditText(this);model.setHint("النموذج (اختياري)");box.addView(model);
        TextView saved=new TextView(this);saved.setTextSize(13);saved.setPadding(dp(8),dp(8),dp(8),dp(8));box.addView(saved);
        Runnable refresh=()->{StringBuilder s=new StringBuilder("المفاتيح المحفوظة:\n");for(String pr:providers){String list=p.getString("keys_"+pr,"");if(!list.isEmpty())s.append("• ").append(pr).append(": ").append(list.split("\\n").length).append("\n");}s.append("النشط: ").append(p.getString("active_provider","لا يوجد"));saved.setText(s.toString());};
        refresh.run();
        provider.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> a){}public void onItemSelected(android.widget.AdapterView<?> a,View v,int pos,long id){String pr=providers[pos];name.setText(pr+" "+(p.getString("keys_"+pr,"").split("\\n").length+1));}});
        Button add=btnDialog("➕ حفظ وتفعيل المفتاح");box.addView(add);
        add.setOnClickListener(v->{String pr=provider.getSelectedItem().toString(),n=name.getText().toString().trim(),k=key.getText().toString().trim(),m=model.getText().toString().trim();if(n.isEmpty())n=pr+" key";if(k.isEmpty()){toast("أدخل مفتاح API أولاً");return;}if(m.isEmpty())m=defaultModel(pr);String old=p.getString("keys_"+pr,""),entry=n+"|"+k+"|"+m,all=old.isEmpty()?entry:old+"\n"+entry;p.edit().putString("keys_"+pr,all).putString("active_provider",pr).putString("active_name",n).putString("active_key",k).putString("active_model",m).apply();key.setText("");refresh.run();toast("تم حفظ وتفعيل المفتاح");});
        Button test=btnDialog("🧪 اختبار الاتصال");box.addView(test);test.setOnClickListener(v->testAi(provider.getSelectedItem().toString(),key.getText().toString().trim(),model.getText().toString().trim()));
        new AlertDialog.Builder(this).setTitle("إعدادات الذكاء الاصطناعي").setView(box).setPositiveButton("إغلاق",null).show();
    }

    Button btnDialog(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
    String defaultModel(String p){if(p.equals("Gemini"))return "gemini-2.5-flash";if(p.equals("OpenRouter"))return "openai/gpt-4o-mini";return "gpt-4o-mini";}

    void testAi(String provider,String key,String model){
        if(key.isEmpty())key=getSharedPreferences("ai_settings",MODE_PRIVATE).getString("active_key","");
        if(key.isEmpty()){toast("أدخل أو احفظ مفتاحاً أولاً");return;}if(model.isEmpty())model=defaultModel(provider);
        final String pr=provider,k=key,m=model;toast("جاري اختبار الاتصال بـ "+pr+"...");new Thread(()->{try{String answer=callAi(pr,k,m,"Reply with exactly: AI_OK");runOnUiThread(()->toast("تم الاتصال: "+answer));}catch(Exception e){runOnUiThread(()->toast("فشل الاتصال: "+e.getMessage()));}}).start();
    }

    void runAi(){
        if(apkFile==null){toast("اختر APK أولاً");return;}
        String request=prompt.getText().toString().trim();if(request.isEmpty()){toast("اكتب طلب التعديل أولاً");return;}
        SharedPreferences p=getSharedPreferences("ai_settings",MODE_PRIVATE);String pr=p.getString("active_provider",""),k=p.getString("active_key",""),m=p.getString("active_model","");
        if(pr.isEmpty()||k.isEmpty()){toast("افتح إعدادات الذكاء الاصطناعي وأضف مفتاحاً");return;}
        status.setText("● جاري الاتصال بـ "+pr+" وتحليل الطلب...");
        report.setText("جاري العمل...\n\nيتم إرسال بنية APK والطلب إلى الذكاء الاصطناعي.");
        final String provider=pr,key=k,model=m;
        new Thread(()->{try{
            String inventory=apkInventory();
            String system="You are an Android APK modification engineer. Analyze the user's requested change against this APK inventory. Return a concise, actionable modification plan naming likely files/classes/resources. Do not claim a change was applied. User request: "+request+"\nAPK inventory:\n"+inventory;
            String answer=callAi(provider,key,model,system);
            runOnUiThread(()->{status.setText("● تم استلام رد الذكاء الاصطناعي");report.setText("خطة الذكاء الاصطناعي:\n\n"+answer+"\n\nهذه النسخة الحالية تعرض الخطة فقط؛ محرك patch/rebuild الكامل لم يُدمج بعد.");});
        }catch(Exception e){runOnUiThread(()->{status.setText("● حدث خطأ");report.setText("تفاصيل الخطأ:\n"+e.getMessage());});}}).start();
    }

    String apkInventory()throws Exception{
        StringBuilder s=new StringBuilder();int files=0,dex=0;try(ZipInputStream z=new ZipInputStream(new FileInputStream(apkFile))){ZipEntry e;while((e=z.getNextEntry())!=null){files++;String n=e.getName();if(n.endsWith(".dex")){dex++;s.append(n).append("\n");}else if(n.equals("AndroidManifest.xml")||n.startsWith("res/")||n.endsWith(".so"))s.append(n).append("\n");}}return "files="+files+", dex="+dex+"\n"+s;}

    String callAi(String provider,String key,String model,String text)throws Exception{
        String url,body;
        if(provider.equals("Gemini")){
            url="https://generativelanguage.googleapis.com/v1beta/models/"+model+":generateContent?key="+java.net.URLEncoder.encode(key,"UTF-8");
            body=new JSONObject().put("contents",new JSONArray().put(new JSONObject().put("parts",new JSONArray().put(new JSONObject().put("text",text))))).toString();
        }else{
            url=provider.equals("OpenRouter")?"https://openrouter.ai/api/v1/chat/completions":"https://api.openai.com/v1/chat/completions";
            body=new JSONObject().put("model",model).put("messages",new JSONArray().put(new JSONObject().put("role","user").put("content",text))).toString();
        }
        HttpURLConnection con=(HttpURLConnection)new URL(url).openConnection();
        con.setRequestMethod("POST");con.setConnectTimeout(30000);con.setReadTimeout(90000);con.setDoOutput(true);con.setRequestProperty("Content-Type","application/json");
        if(!provider.equals("Gemini"))con.setRequestProperty("Authorization","Bearer "+key);
        if(provider.equals("OpenRouter")){con.setRequestProperty("HTTP-Referer","https://github.com/proamine12345-bot/morphe-ai99998887776665544455");con.setRequestProperty("X-Title","APK AI Lab");}
        try(OutputStream out=con.getOutputStream()){out.write(body.getBytes("UTF-8"));}
        int code=con.getResponseCode();InputStream stream=code>=400?con.getErrorStream():con.getInputStream();String raw=readAll(stream);
        if(code<200||code>=300)throw new IOException("HTTP "+code+": "+raw);
        JSONObject j=new JSONObject(raw);
        if(provider.equals("Gemini"))return j.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
        return j.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
    }

    String readAll(InputStream in)throws Exception{if(in==null)return "";try(BufferedReader r=new BufferedReader(new InputStreamReader(in,"UTF-8"))){StringBuilder s=new StringBuilder();String line;while((line=r.readLine())!=null)s.append(line);return s.toString();}}
    void pickApk(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/vnd.android.package-archive");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_APK);}
    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==PICK_APK&&c==RESULT_OK&&d!=null){apkUri=d.getData();try{apkFile=new File(getCacheDir(),"input.apk");copy(apkUri,apkFile);apkName.setText("تم اختيار APK ✓");apkMeta.setText("الحجم: "+(apkFile.length()/1024)+" كيلوبايت");status.setText("● APK جاهز للتحليل");analyze();}catch(Exception e){status.setText("● فشل اختيار APK");report.setText(e.getMessage());}}}
    void copy(Uri u,File out)throws Exception{try(InputStream in=getContentResolver().openInputStream(u);OutputStream o=new FileOutputStream(out)){byte[] b=new byte[8192];int n;while((n=in.read(b))>0)o.write(b,0,n);}}
    void analyze(){if(apkFile==null){toast("اختر APK أولاً");return;}try{int dex=0,files=0;StringBuilder names=new StringBuilder();try(ZipInputStream z=new ZipInputStream(new FileInputStream(apkFile))){ZipEntry e;while((e=z.getNextEntry())!=null){files++;String n=e.getName();if(n.endsWith(".dex")){dex++;names.append(n).append("\n");}}}report.setText("نتيجة التحليل الأولي\n\nالملفات: "+files+"\nملفات DEX: "+dex+"\n\n"+names);status.setText("● اكتمل التحليل الأولي");}catch(Exception e){report.setText("فشل التحليل: "+e);}}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}