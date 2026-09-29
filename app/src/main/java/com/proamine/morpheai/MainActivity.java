package com.proamine.morpheai;

import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.graphics.Typeface;
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
    LinearLayout root; TextView status, report; EditText prompt; Uri apkUri; File apkFile;

    int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    TextView tv(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setPadding(dp(14),dp(10),dp(14),dp(10));return t;}
    Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}

    @Override public void onCreate(Bundle b){super.onCreate(b);build();}

    void build(){
        ScrollView sv=new ScrollView(this); root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14),dp(18),dp(14),dp(18));sv.addView(root);setContentView(sv);
        TextView title=tv("🤖 APK AI Lab",25);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);root.addView(title);
        root.addView(tv("AI APK Lab • Gemini / OpenAI / OpenRouter",15));
        Button settings=btn("⚙️ إعدادات AI — المفاتيح والمزودون");root.addView(settings);settings.setOnClickListener(v->showAiSettings());
        Button pick=btn("📦 اختيار APK من الهاتف");root.addView(pick);pick.setOnClickListener(v->pickApk());
        status=tv("لم يتم اختيار APK",14);root.addView(status);report=tv("",14);root.addView(report);
        root.addView(tv("طلب التعديل للـAI",18));
        prompt=new EditText(this);prompt.setHint("مثال: غيّر اسم التطبيق إلى Ayman Game");prompt.setMinLines(4);prompt.setGravity(Gravity.TOP);
        root.addView(prompt,new LinearLayout.LayoutParams(-1,dp(130)));
        Button analyze=btn("🧠 تحليل APK");root.addView(analyze);analyze.setOnClickListener(v->analyze());
        Button run=btn("⚡ إرسال الطلب إلى AI وتنفيذ المرحلة");root.addView(run);run.setOnClickListener(v->runAi());
        root.addView(tv("سيظهر سجل الاتصال والرد هنا. الاتصال الفعلي يستخدم المفتاح المحفوظ.",13));
    }

    void showAiSettings(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(8),dp(4),dp(8),dp(4));
        SharedPreferences p=getSharedPreferences("ai_settings",MODE_PRIVATE);
        String[] providers={"Gemini","OpenAI","OpenRouter"};
        Spinner provider=new Spinner(this);provider.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,providers));
        box.addView(tv("مزود AI",15));box.addView(provider);
        EditText name=new EditText(this);name.setHint("اسم المفتاح");box.addView(name);
        EditText key=new EditText(this);key.setHint("API Key");key.setInputType(129);box.addView(key);
        EditText model=new EditText(this);model.setHint("النموذج (اختياري)");box.addView(model);
        TextView saved=tv("",13);box.addView(saved);
        Runnable refresh=()->{StringBuilder s=new StringBuilder("🔐 المحفوظ:\n");for(String pr:providers){String list=p.getString("keys_"+pr,"");if(!list.isEmpty())s.append("• ").append(pr).append(": ").append(list.split("\\n").length).append("\n");}s.append("النشط: ").append(p.getString("active_provider","لا يوجد"));saved.setText(s.toString());};refresh.run();
        provider.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> a){}public void onItemSelected(android.widget.AdapterView<?> a,View v,int pos,long id){String pr=providers[pos];name.setText(pr+" "+(p.getString("keys_"+pr,"").split("\\n").length+1));}});
        Button add=btn("➕ حفظ وتفعيل المفتاح");box.addView(add);
        add.setOnClickListener(v->{String pr=provider.getSelectedItem().toString(),n=name.getText().toString().trim(),k=key.getText().toString().trim(),m=model.getText().toString().trim();if(n.isEmpty())n=pr+" key";if(k.isEmpty()){toast("أدخل API Key");return;}if(m.isEmpty())m=defaultModel(pr);String old=p.getString("keys_"+pr,""),entry=n+"|"+k+"|"+m,all=old.isEmpty()?entry:old+"\n"+entry;p.edit().putString("keys_"+pr,all).putString("active_provider",pr).putString("active_name",n).putString("active_key",k).putString("active_model",m).apply();key.setText("");refresh.run();toast("✅ تم حفظ وتفعيل "+pr);});
        Button test=btn("🧪 اختبار الاتصال الآن");box.addView(test);test.setOnClickListener(v->testAi(provider.getSelectedItem().toString(),key.getText().toString().trim(),model.getText().toString().trim()));
        new AlertDialog.Builder(this).setTitle("🤖 إعدادات AI").setView(box).setPositiveButton("إغلاق",null).show();
    }

    String defaultModel(String p){if(p.equals("Gemini"))return "gemini-2.5-flash";if(p.equals("OpenRouter"))return "openai/gpt-4o-mini";return "gpt-4o-mini";}
    void testAi(String provider,String key,String model){
        if(key.isEmpty()){key=getSharedPreferences("ai_settings",MODE_PRIVATE).getString("active_key","");}
        if(key.isEmpty()){toast("أدخل أو احفظ مفتاحًا أولاً");return;} if(model.isEmpty())model=defaultModel(provider);
        final String pr=provider,k=key,m=model;toast("⏳ اختبار "+pr+"...");new Thread(()->{try{String answer=callAi(pr,k,m,"Reply with exactly: AI_OK");runOnUiThread(()->toast("✅ "+pr+" متصل: "+answer));}catch(Exception e){runOnUiThread(()->toast("❌ فشل الاتصال: "+e.getMessage()));}}).start();
    }

    void runAi(){
        if(apkFile==null){toast("اختر APK أولاً");return;}
        String request=prompt.getText().toString().trim();if(request.isEmpty()){toast("اكتب طلب التعديل أولاً");return;}
        SharedPreferences p=getSharedPreferences("ai_settings",MODE_PRIVATE);String pr=p.getString("active_provider",""),k=p.getString("active_key",""),m=p.getString("active_model","");
        if(pr.isEmpty()||k.isEmpty()){toast("افتح إعدادات AI وأضف مفتاحًا");return;}
        status.setText("⏳ جارٍ الاتصال بـ "+pr+" وتحليل طلبك...");
        report.setText("AI يعمل الآن...\nجارٍ إرسال وصف APK وطلبك إلى المزود.");
        final String provider=pr,key=k,model=m;
        new Thread(()->{try{
            String inventory=apkInventory();
            String system="You are an Android APK modification engineer. Analyze the user's requested change against this APK inventory. Return a concise, actionable modification plan naming likely files/classes/resources. Do not claim a change was applied. User request: "+request+"\nAPK inventory:\n"+inventory;
            String answer=callAi(provider,key,model,system);
            runOnUiThread(()->{status.setText("✅ AI استجاب بنجاح عبر "+provider);report.setText("🧠 رد AI:\n\n"+answer+"\n\n⚠️ هذه المرحلة تستقبل خطة AI. التنفيذ الفعلي وإعادة بناء APK يحتاجان محرك patch/rebuild داخل التطبيق.");});
        }catch(Exception e){runOnUiThread(()->{status.setText("❌ فشل AI");report.setText("خطأ الاتصال:\n"+e.getMessage());});}}).start();
    }

    String apkInventory()throws Exception{
        StringBuilder s=new StringBuilder();int files=0,dex=0;try(ZipInputStream z=new ZipInputStream(new FileInputStream(apkFile))){ZipEntry e;while((e=z.getNextEntry())!=null){files++;String n=e.getName();if(n.endsWith(".dex")){dex++;s.append(n).append("\n");}else if(n.equals("AndroidManifest.xml")||n.startsWith("res/")||n.endsWith(".so"))s.append(n).append("\n");}}return "files="+files+", dex="+dex+"\n"+s;}

    String callAi(String provider,String key,String model,String text)throws Exception{
        String url;
        String body;
        if(provider.equals("Gemini")){
            url="https://generativelanguage.googleapis.com/v1beta/models/"+model+":generateContent?key="+java.net.URLEncoder.encode(key,"UTF-8");
            body=new JSONObject().put("contents",new JSONArray().put(new JSONObject().put("parts",new JSONArray().put(new JSONObject().put("text",text))))).toString();
        }else{
            url=provider.equals("OpenRouter")?"https://openrouter.ai/api/v1/chat/completions":"https://api.openai.com/v1/chat/completions";
            body=new JSONObject().put("model",model).put("messages",new JSONArray().put(new JSONObject().put("role","user").put("content",text))).toString();
        }
        HttpURLConnection con=(HttpURLConnection)new URL(url).openConnection();
        con.setRequestMethod("POST"); con.setConnectTimeout(30000); con.setReadTimeout(90000); con.setDoOutput(true);
        con.setRequestProperty("Content-Type","application/json");
        if(!provider.equals("Gemini")) con.setRequestProperty("Authorization","Bearer "+key);
        if(provider.equals("OpenRouter")){
            con.setRequestProperty("HTTP-Referer","https://github.com/proamine12345-bot/morphe-ai99998887776665544455");
            con.setRequestProperty("X-Title","APK AI Lab");
        }
        try(OutputStream out=con.getOutputStream()){out.write(body.getBytes("UTF-8"));}
        int code=con.getResponseCode();
        InputStream stream=code>=400?con.getErrorStream():con.getInputStream();
        String raw=readAll(stream);
        if(code<200||code>=300) throw new IOException("HTTP "+code+": "+raw);
        JSONObject j=new JSONObject(raw);
        if(provider.equals("Gemini")) return j.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
        return j.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
    }

    String readAll(InputStream in)throws Exception{
        if(in==null)return "";
        try(BufferedReader r=new BufferedReader(new InputStreamReader(in,"UTF-8"))){
            StringBuilder s=new StringBuilder(); String line;
            while((line=r.readLine())!=null)s.append(line);
            return s.toString();
        }
    }

    void pickApk(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/vnd.android.package-archive");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_APK);}
    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==PICK_APK&&c==RESULT_OK&&d!=null){apkUri=d.getData();try{apkFile=new File(getCacheDir(),"input.apk");copy(apkUri,apkFile);status.setText("✅ APK جاهز: "+apkFile.length()/1024+" KB");analyze();}catch(Exception e){status.setText("❌ "+e.getMessage());}}}
    void copy(Uri u,File out)throws Exception{try(InputStream in=getContentResolver().openInputStream(u);OutputStream o=new FileOutputStream(out)){byte[] b=new byte[8192];int n;while((n=in.read(b))>0)o.write(b,0,n);}}
    void analyze(){if(apkFile==null){toast("اختر APK أولاً");return;}try{int dex=0,files=0;StringBuilder names=new StringBuilder();try(ZipInputStream z=new ZipInputStream(new FileInputStream(apkFile))){ZipEntry e;while((e=z.getNextEntry())!=null){files++;String n=e.getName();if(n.endsWith(".dex")){dex++;names.append(n).append("\n");}}}report.setText("🔬 تحليل APK\nالملفات: "+files+"\nDEX: "+dex+"\n"+names);}catch(Exception e){report.setText("❌ فشل التحليل: "+e);}}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}