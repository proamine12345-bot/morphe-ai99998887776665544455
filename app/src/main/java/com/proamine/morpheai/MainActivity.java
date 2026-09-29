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
    LinearLayout root, chat;
    TextView status, report;
    EditText prompt;
    Uri apkUri;
    File apkFile;

    int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    TextView tv(String s,int size){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setPadding(dp(14),dp(10),dp(14),dp(10)); return t; }
    Button btn(String s){ Button b=new Button(this); b.setText(s); b.setAllCaps(false); return b; }

    @Override public void onCreate(Bundle b){ super.onCreate(b); build(); }
    void build(){
        ScrollView sv=new ScrollView(this); root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(14),dp(18),dp(14),dp(18)); sv.addView(root); setContentView(sv);
        TextView title=tv("🤖 Morphe AI Mobile",25); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); root.addView(title);
        root.addView(tv("AI APK Lab • تحليل APK + DEX/Smali + تنفيذ التعديلات",15));
        Button pick=btn("📦 رفع APK مباشرة من الهاتف"); root.addView(pick); pick.setOnClickListener(v->pickApk());
        status=tv("لم يتم اختيار APK",14); root.addView(status);
        report=tv("",14); root.addView(report);
        root.addView(tv("طلبك للـAI",18));
        prompt=new EditText(this); prompt.setHint("مثال: غيّر اسم التطبيق والواجهة والألوان..."); prompt.setMinLines(4); prompt.setGravity(Gravity.TOP); root.addView(prompt,new LinearLayout.LayoutParams(-1,dp(130)));
        Button analyze=btn("🧠 تحليل APK بعمق"); root.addView(analyze); analyze.setOnClickListener(v->analyze());
        Button run=btn("⚙️ تنفيذ التعديل وإعادة البناء"); root.addView(run); run.setOnClickListener(v->showPlan());
        Button download=btn("⬇️ تنزيل APK المعدل"); root.addView(download); download.setOnClickListener(v->Toast.makeText(this,"سيظهر زر التنزيل بعد نجاح إعادة البناء.",Toast.LENGTH_LONG).show());
        root.addView(tv("ملاحظة: هذه النسخة لا تدّعي وجود AI سحابي مدمج بدون مفتاح/API. التحليل الأساسي يتم محليًا، ويمكن ربط مزود AI لاحقًا.",13));
    }
    void pickApk(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("application/vnd.android.package-archive"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,PICK_APK); }
    @Override protected void onActivityResult(int r,int c,Intent d){ super.onActivityResult(r,c,d); if(r==PICK_APK && c==RESULT_OK && d!=null){ apkUri=d.getData(); try{ apkFile=new File(getCacheDir(),"input.apk"); copy(apkUri,apkFile); status.setText("✅ APK جاهز للتحليل: "+apkFile.length()/1024+" KB"); analyze(); }catch(Exception e){status.setText("❌ "+e.getMessage());} } }
    void copy(Uri u,File out)throws Exception{ try(InputStream in=getContentResolver().openInputStream(u); OutputStream o=new FileOutputStream(out)){ byte[] b=new byte[8192]; int n; while((n=in.read(b))>0)o.write(b,0,n); } }
    void analyze(){
        if(apkFile==null){Toast.makeText(this,"اختر APK أولاً",Toast.LENGTH_SHORT).show();return;}
        try{
            int dex=0, files=0; long bytes=0; StringBuilder names=new StringBuilder();
            try(ZipInputStream z=new ZipInputStream(new FileInputStream(apkFile))){ ZipEntry e; while((e=z.getNextEntry())!=null){ files++; if(e.getSize()>0)bytes+=e.getSize(); String n=e.getName(); if(n.endsWith(".dex")){dex++; names.append(n).append("\n");} } }
            report.setText("🔬 تحليل أولي ناجح\nالملفات: "+files+"\nDEX: "+dex+"\nDEX files:\n"+names+"\nالخطوة التالية: تحديد الهدف من طلبك ثم توليد patch آمن.");
        }catch(Exception e){report.setText("❌ فشل التحليل: "+e);}
    }
    void showPlan(){
        if(apkFile==null){Toast.makeText(this,"اختر APK أولاً",Toast.LENGTH_SHORT).show();return;}
        new AlertDialog.Builder(this).setTitle("خطة التعديل").setMessage("الطلب:\n"+prompt.getText().toString()+"\n\nسيحتاج التنفيذ الفعلي إلى محرك AI متصل بمفتاح API أو Agent محلي؛ لن يتم الادعاء بتعديل APK إذا لم يتم توليد patch قابل للتحقق.").setPositiveButton("حسنًا",null).show();
    }
}
