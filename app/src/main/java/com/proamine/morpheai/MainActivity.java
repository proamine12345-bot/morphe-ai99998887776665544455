package com.proamine.morpheai;

import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class MainActivity extends Activity {
    static final int PICK_APK=41;
    LinearLayout root;
    TextView status, report;
    EditText prompt;
    Uri apkUri;
    File apkFile;

    int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    TextView tv(String s,int size){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setPadding(dp(14),dp(10),dp(14),dp(10)); return t; }
    Button btn(String s){ Button b=new Button(this); b.setText(s); b.setAllCaps(false); return b; }

    @Override public void onCreate(Bundle b){ super.onCreate(b); build(); }

    void build(){
        ScrollView sv=new ScrollView(this);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14),dp(18),dp(14),dp(18)); sv.addView(root); setContentView(sv);

        TextView title=tv("🤖 Morphe AI Mobile",25); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); root.addView(title);
        root.addView(tv("AI APK Lab • تحليل APK + DEX/Smali + تنفيذ التعديلات",15));

        Button settings=btn("⚙️ إعدادات AI — المفاتيح والمزودون");
        root.addView(settings);
        settings.setOnClickListener(v->showAiSettings());

        Button pick=btn("📦 رفع APK مباشرة من الهاتف"); root.addView(pick); pick.setOnClickListener(v->pickApk());
        status=tv("لم يتم اختيار APK",14); root.addView(status);
        report=tv("",14); root.addView(report);

        root.addView(tv("طلبك للـAI",18));
        prompt=new EditText(this); prompt.setHint("مثال: غيّر اسم التطبيق والواجهة والألوان...");
        prompt.setMinLines(4); prompt.setGravity(Gravity.TOP);
        root.addView(prompt,new LinearLayout.LayoutParams(-1,dp(130)));

        Button analyze=btn("🧠 تحليل APK بعمق"); root.addView(analyze); analyze.setOnClickListener(v->analyze());
        Button run=btn("⚙️ تنفيذ التعديل وإعادة البناء"); root.addView(run); run.setOnClickListener(v->showPlan());
        Button download=btn("⬇️ تنزيل APK المعدل"); root.addView(download);
        download.setOnClickListener(v->Toast.makeText(this,"سيظهر زر التنزيل بعد نجاح إعادة البناء.",Toast.LENGTH_LONG).show());

        root.addView(tv("يمكنك حفظ عدة مفاتيح Gemini وOpenAI وOpenRouter. المفتاح المختار يصبح المزود النشط للـAI.",13));
    }

    void showAiSettings(){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(8),dp(4),dp(8),dp(4));
        SharedPreferences p=getSharedPreferences("ai_settings",MODE_PRIVATE);

        Spinner provider=new Spinner(this);
        String[] providers={"Gemini","OpenAI","OpenRouter"};
        provider.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,providers));
        box.addView(tv("مزود AI",15)); box.addView(provider);

        EditText name=new EditText(this); name.setHint("اسم المفتاح، مثال: Gemini 1"); box.addView(name);
        EditText key=new EditText(this); key.setHint("API Key"); key.setInputType(129); box.addView(key);

        TextView saved=tv("",13); box.addView(saved);

        Runnable refresh=()->{
            StringBuilder s=new StringBuilder("🔐 المفاتيح المحفوظة:\n");
            for(String pr:providers){
                String list=p.getString("keys_"+pr,"");
                if(!list.isEmpty()) s.append("• ").append(pr).append(": ").append(list.split("\\n").length).append(" مفتاح/مفاتيح\n");
            }
            s.append("المزود النشط: ").append(p.getString("active_provider","Gemini"));
            saved.setText(s.toString());
        };
        refresh.run();

        provider.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(android.widget.AdapterView<?> a){}
            public void onItemSelected(android.widget.AdapterView<?> a,View v,int pos,long id){
                String pr=providers[pos]; name.setText(pr+" "+(p.getString("keys_"+pr,"").split("\\n").length+1));
            }
        });

        Button add=btn("➕ حفظ المفتاح"); box.addView(add);
        add.setOnClickListener(v->{
            String pr=provider.getSelectedItem().toString(), n=name.getText().toString().trim(), k=key.getText().toString().trim();
            if(n.isEmpty()) n=pr+" key";
            if(k.isEmpty()){Toast.makeText(this,"أدخل API Key أولاً",Toast.LENGTH_SHORT).show();return;}
            String old=p.getString("keys_"+pr,"");
            String entry=n+"|"+k;
            String all=old.isEmpty()?entry:old+"\n"+entry;
            p.edit().putString("keys_"+pr,all).putString("active_provider",pr).putString("active_name",n).putString("active_key",k).apply();
            key.setText(""); refresh.run();
            Toast.makeText(this,"✅ تم حفظ المفتاح وتفعيله",Toast.LENGTH_SHORT).show();
        });

        Button activate=btn("✅ جعل المفتاح المختار نشطًا"); box.addView(activate);
        activate.setOnClickListener(v->{
            String pr=provider.getSelectedItem().toString(), k=key.getText().toString().trim(), n=name.getText().toString().trim();
            if(k.isEmpty()){Toast.makeText(this,"أدخل المفتاح المراد تفعيله",Toast.LENGTH_SHORT).show();return;}
            p.edit().putString("active_provider",pr).putString("active_name",n).putString("active_key",k).apply();
            refresh.run(); Toast.makeText(this,"✅ AI الآن يستخدم "+pr,Toast.LENGTH_SHORT).show();
        });

        Button test=btn("🧪 اختبار المفتاح"); box.addView(test);
        test.setOnClickListener(v->Toast.makeText(this,"تم حفظ المفتاح. اختبار الاتصال الفعلي يحتاج طبقة HTTP للمزود.",Toast.LENGTH_LONG).show());

        new AlertDialog.Builder(this).setTitle("🤖 إعدادات AI").setView(box).setPositiveButton("إغلاق",null).show();
    }

    void pickApk(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("application/vnd.android.package-archive"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,PICK_APK); }
    @Override protected void onActivityResult(int r,int c,Intent d){ super.onActivityResult(r,c,d); if(r==PICK_APK && c==RESULT_OK && d!=null){ apkUri=d.getData(); try{ apkFile=new File(getCacheDir(),"input.apk"); copy(apkUri,apkFile); status.setText("✅ APK جاهز للتحليل: "+apkFile.length()/1024+" KB"); analyze(); }catch(Exception e){status.setText("❌ "+e.getMessage());} } }
    void copy(Uri u,File out)throws Exception{ try(InputStream in=getContentResolver().openInputStream(u); OutputStream o=new FileOutputStream(out)){ byte[] b=new byte[8192]; int n; while((n=in.read(b))>0)o.write(b,0,n); } }
    void analyze(){
        if(apkFile==null){Toast.makeText(this,"اختر APK أولاً",Toast.LENGTH_SHORT).show();return;}
        try{
            int dex=0, files=0; StringBuilder names=new StringBuilder();
            try(ZipInputStream z=new ZipInputStream(new FileInputStream(apkFile))){ ZipEntry e; while((e=z.getNextEntry())!=null){ files++; String n=e.getName(); if(n.endsWith(".dex")){dex++; names.append(n).append("\n");} } }
            report.setText("🔬 تحليل أولي ناجح\nالملفات: "+files+"\nDEX: "+dex+"\nDEX files:\n"+names+"\nالخطوة التالية: تحديد الهدف من طلبك ثم توليد patch آمن.");
        }catch(Exception e){report.setText("❌ فشل التحليل: "+e);}
    }
    void showPlan(){
        if(apkFile==null){Toast.makeText(this,"اختر APK أولاً",Toast.LENGTH_SHORT).show();return;}
        String active=getSharedPreferences("ai_settings",MODE_PRIVATE).getString("active_provider","");
        if(active.isEmpty()){Toast.makeText(this,"افتح إعدادات AI وأضف مفتاحًا أولاً",Toast.LENGTH_LONG).show();return;}
        new AlertDialog.Builder(this).setTitle("خطة التعديل").setMessage("المزود النشط: "+active+"\n\nالطلب:\n"+prompt.getText().toString()+"\n\nسيتم ربط هذا الإعداد بطبقة AI الفعلية في مرحلة التنفيذ.").setPositiveButton("حسنًا",null).show();
    }
}