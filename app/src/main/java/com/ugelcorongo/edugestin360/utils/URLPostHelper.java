package com.ugelcorongo.edugestin360.utils;

public class URLPostHelper {
    public static class Colegio {
        public static final String VER = "https://ugelcorongo.com/api/colegio/ver";
        public static final String REGISTRAR = "https://ugelcorongo.com/api/colegio/registrar";
        public static final String ACTUALIZAR = "https://ugelcorongo.com/api/colegio/actualizar";
        public static final String ELIMINAR = "https://ugelcorongo.com/api/colegio/eliminar";
        public static final String INFO = "https://…/getDatainfoColegio.php";
    }

    public static class Director {
        public static final String Asistencia = "https://ugelcorongo.pe/monitoreo/app/asistencia/director_asistencia.php";
        public static final String Docentes = "https://ugelcorongo.pe/monitoreo/app/data/director_docentes.php";
        public static final String Colegio = "https://ugelcorongo.pe/monitoreo/app/data/datacolegio_director.php";
        public static final String Registros = "https://ugelcorongo.pe/monitoreo/app/data/director_registros.php";

        public static final String ListDocentes    = "https://ugelcorongo.pe/monitoreo/app/crud/director_docentes.php";
        public static final String GetDocenteByDni = "https://ugelcorongo.pe/monitoreo/app/crud/get_docente_por_dni.php";
        public static final String CreateDocente   = "https://ugelcorongo.pe/monitoreo/app/crud/create_docente.php";
        public static final String UpdateDocente   = "https://ugelcorongo.pe/monitoreo/app/crud/update_docente.php";
        public static final String DeleteDocente = "https://ugelcorongo.pe/monitoreo/app/crud/delete_docente.php";
    }

    public static class Asistencia {
        public static final String VER = "https://ugelcorongo.com/api/asistencia/ver";
        public static final String REGISTRAR = "https://ugelcorongo.pe/monitoreo/app/asistencia/registro_asistencia.php";
        public static final String ACTUALIZAR = "https://ugelcorongo.com/api/asistencia/actualizar";
        public static final String ELIMINAR = "https://ugelcorongo.com/api/asistencia/eliminar";
    }

    public static class PDF {
        public static final String VER = "https://ugelcorongo.com/api/pdf/ver";
        public static final String REGISTRAR = "https://ugelcorongo.pe/monitoreo/app/pdf/registrar_pdf.php";
        public static final String ACTUALIZAR = "https://ugelcorongo.com/api/pdf/actualizar";
        public static final String ELIMINAR = "https://ugelcorongo.com/api/pdf/eliminar";
    }

    public static class Imagen {
        public static final String VER = "https://ugelcorongo.com/api/imagen/ver";
        public static final String REGISTRAR = "https://ugelcorongo.pe/monitoreo/app/img/registrar_img.php";
        public static final String ACTUALIZAR = "https://ugelcorongo.com/api/imagen/actualizar";
        public static final String ELIMINAR = "https://ugelcorongo.com/api/imagen/eliminar";
        public static final String IMG = "https://ugelcorongo.pe/monitoreo/app/ficha/uploads/";
    }

    public static class Fichas {
        public static final String VER = "https://ugelcorongo.pe/monitoreo/app/data/datafichas.php";
        public static final String REGISTRAR = "https://ugelcorongo.pe/monitoreo/app/ficha/registrar_ficha.php";
        public static final String DocentesEnFichas = "https://ugelcorongo.pe/monitoreo/app/data/docentes_en_fichas.php?idFicha=%s&visita=%s";
        public static final String ACTUALIZAR = "https://ugelcorongo.com/api/ficha/actualizar";
        public static final String ELIMINAR = "https://ugelcorongo.com/api/ficha/eliminar";
        public static final String LIST_SUBMISSIONS = "https://ugelcorongo.pe/monitoreo/app/ficha/list_submissions.php";
        public static final String LIST_RESPONSES = "https://ugelcorongo.pe/monitoreo/app/ficha/list_responses.php?encabezado_id=%s";
        public static final String LIST_QUESTIONS = "https://ugelcorongo.pe/monitoreo/app/ficha/listar_preguntas_por_ficha.php";
        public static final String LIST_QUESTIONS_GET = "https://ugelcorongo.pe/monitoreo/app/ficha/listar_preguntas_por_ficha_get.php?idficha=%s";
    }

    public static class Coordenadas {
        public static final String VER = "https://ugelcorongo.com/api/colegio/ver";
        public static final String REGISTRAR = "https://ugelcorongo.pe/monitoreo/app/rastreo/registrar_rastreo.php";
        public static final String ACTUALIZAR = "https://ugelcorongo.com/api/colegio/actualizar";
        public static final String ELIMINAR = "https://ugelcorongo.com/api/colegio/eliminar";
    }

    public static class Data {
        public static final String DocentesInfo = "https://ugelcorongo.pe/monitoreo/app/usuario/docenteInfo.php";
        public static final String Colegios = "https://ugelcorongo.pe/monitoreo/app/data/datacolegio.php";
        public static final String EspecialistasInfo = "https://ugelcorongo.pe/monitoreo/app/data/info_especialistas.php";
        public static final String Fichas = "https://ugelcorongo.pe/monitoreo/app/data/datafichas.php";
    }

    public static class Preguntas {
        public static final String VER = "https://ugelcorongo.pe/monitoreo/app/ficha/datapreguntas.php";
    }

    public static class Terminos {
        public static final String Info = "https://docs.google.com/document/d/1KC8OlxK7tRs1FLX5EQJeWNRnHv4nM_vG/edit?usp=drive_link&ouid=111126020909884026673&rtpof=true&sd=true";
    }

    public static class Respuestas {
        public static final String VER = "https://ugelcorongo.pe/monitoreo/app/ficha/datarespuestas.php";
    }

    public static class Usuarios {
        public static String CONSULTAR(String userId) {
            return "https://ugelcorongo.pe/monitoreo/app/usuario/getInfoById.php?userId=" + userId;
        }
    }

    public static class Login {
        public static String Verificar() {
            return "https://ugelcorongo.pe/monitoreo/app/login/getUsuarios.php";
        }
        public static String CambiarPassword() {
            return "https://ugelcorongo.pe/monitoreo/app/login/updateContrasenia.php";
        }
        public static String PasswordCambiada(String userId) {
            return "attendanceapp://change-password?userId=" + userId;
        }

        public static String PasswordCambiadaOK() {
            return "attendanceapp://login";
        }
    }
}