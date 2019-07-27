package ie.ucd.smartrideRT;

import android.os.Environment;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Upload {
    private static final String ACCESS_TOKEN = "lY_d3DAmzgAAAAAAAAAAp28xHJrK_8n7JbxEbxVoSvlaR9_ABuES6K8DvB8Jyb75";

    private static final String dataLabel = "EBike";

    public static void upload() throws DbxException, IOException {

        /**
         * Create Dropbox client
         */
        DbxRequestConfig config = new DbxRequestConfig("dropbox/SmartRide_RealTime");
        DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);


        /**
         * Get current account info
         */

        /*
        FullAccount account = client.users().getCurrentAccount();
        System.out.println(account.getName().getDisplayName());
        */

        /**
         * Get files and folder metadata from Dropbox root directory
        */
        /*
        ListFolderResult result = client.files().listFolder("");
        while (true) {
            for (Metadata metadata : result.getEntries()) {
                System.out.println(metadata.getPathLower());
            }

            if (!result.getHasMore()) {
                break;
            }

            result = client.files().listFolderContinue(result.getCursor());
        }
        */

        /**
         * Upload data file to Dropbox
         */


        //Environment.getDataDirectory().getPath()+"/data/ie.ucd.smartride/databases/data.db");


        try (InputStream in = new FileInputStream(Environment.getDataDirectory().getPath()+"/data/ie.ucd.smartride/databases/data.db");
        ) {
            FileMetadata metadata = client.files().uploadBuilder("/"+dataLabel)
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(in);
        }
        catch (DbxException ex) {
            System.out.println(ex.getMessage());
        }

    }
}