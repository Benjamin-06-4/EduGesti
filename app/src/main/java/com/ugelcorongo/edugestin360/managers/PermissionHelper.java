package com.ugelcorongo.edugestin360.managers;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.FragmentActivity;

public class PermissionHelper {

    /**
     * Callback para el resultado de la petición de permisos.
     */
    public interface PermissionCallback {
        /**
         * @param granted true si TODOS los permisos fueron concedidos; false en caso contrario.
         */
        void onResult(boolean granted);
    }
    public static void request(FragmentActivity act,
                               String[] perms, int code, PermissionCallback cb){
        ActivityResultLauncher<String[]> launcher =
                act.registerForActivityResult(
                        new ActivityResultContracts.RequestMultiplePermissions(),
                        results->{
                            boolean ok = results.values().stream().allMatch(b->b);
                            cb.onResult(ok);
                        });
        launcher.launch(perms);
    }
}