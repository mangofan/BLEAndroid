package code.source.es.newbluetooth.utils;
/*
 * Created by fanwe on 2017/3/28.
 */

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileCache {

   public synchronized  void saveFile(String saveString){

        File f = new File(Environment.getExternalStorageDirectory(),"/download/db.txt");
        FileOutputStream out =null;
//       if(f.exists()) {
//           Log.d("tag", f.getAbsolutePath());
//           InputStream in = null;
//           try {
//               in = new FileInputStream(f);
//               byte[] buff=new byte[1024];
//               int len;
//               while ((len=in.read(buff))>0) {
//                   in.read(buff,0,len);
//                   Log.d("tag",buff.toString());
//               }
//           } catch (Exception e) {
//               e.printStackTrace();
//           }finally {
//               if (in!=null)
//                   try {
//                       in.close();
//                   } catch (IOException e) {
//                       e.printStackTrace();
//                   }
//           }
//       }

        try {
            out =new FileOutputStream(f,true);
            out.write(saveString.getBytes());
            out.write("\n".getBytes());
            out.flush();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if(out!=null)
                    out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
