package ninja.duck.smoothlife;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

import org.bytedeco.fftw.global.fftw3;
import org.bytedeco.javacpp.DoublePointer;

import java.util.Random;

import androidx.preference.PreferenceManager;

public class SLWallpaperService extends WallpaperService {
    private static final String TAG = "WallpaperService";

    public SLWallpaperService() {
    }

    @Override
    public Engine onCreateEngine() {
        Log.d(TAG, "Creating engine");
        return new SLWallpaperEngine();
    }
    private class SLWallpaperEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {
        private boolean visible;
        int width;
        int height;
        int last_width = 0;
        int last_height = 0;
        int actual_width;
        int actual_height;
        double field[];
        int pixels[];
        Rules rules;
        Multipliers multipliers;
        Paint paint;
        Bitmap bitmap[];
        int bitmap_index = 0;
        int dead_count = 0;

        fftw3.fftw_plan plan_forward = null;
        fftw3.fftw_plan plan_reverse = null;
        DoublePointer doublepointer_in_forward;
        DoublePointer doublepointer_out_forward;
        DoublePointer doublepointer_in_reverse_m;
        DoublePointer doublepointer_out_reverse_m;
        DoublePointer doublepointer_in_reverse_n;
        DoublePointer doublepointer_out_reverse_n;

        ColorMap cmap;
        int color_scaling;
        int scale = 4;
        int frame_delay;
        double dt;
        double last_dt = -2;
        double inner_radius = 7.0;
        double outer_radius = 3*inner_radius;

        SharedPreferences prefs;
        String last_pref_value = "";
        private int frame_counter = 0;
        private long frame_millis_sum = 0;


        private void make_plans() {
            Log.d(TAG, "Making plans");
            fftw3.fftw_set_timelimit(2.0);
            Log.d(TAG, "Creating double pointers");
            doublepointer_in_forward = new DoublePointer(height * width);
            doublepointer_out_forward = new DoublePointer(height * (width/2 + 1) * 2);
            Log.d(TAG, "Creating forward plan");
            plan_forward = fftw3.fftw_plan_dft_r2c_2d(height, width, doublepointer_in_forward, doublepointer_out_forward, (int)fftw3.FFTW_MEASURE);

            Log.d(TAG, "Creating more double pointers");
            doublepointer_in_reverse_m = new DoublePointer(height * (width/2 + 1) * 2);
            doublepointer_out_reverse_m = new DoublePointer(height * width);
            doublepointer_in_reverse_n = new DoublePointer(height * (width/2 + 1) * 2);
            doublepointer_out_reverse_n = new DoublePointer(height * width);
            Log.d(TAG, "Creating reverse plan");
            plan_reverse = fftw3.fftw_plan_dft_c2r_2d(height, width, doublepointer_in_reverse_m, doublepointer_out_reverse_m, (int)fftw3.FFTW_MEASURE);
        }

        private void delete_plans() {
            if(plan_forward != null) {
                Log.d(TAG, "Deleting plans");
                fftw3.fftw_destroy_plan(plan_forward);
                Log.d(TAG, "Forward plan deleted");
                fftw3.fftw_destroy_plan(plan_reverse);
                Log.d(TAG, "Reverse plan deleted");
            }
        }

        private class Rules {
            double B1 = 0.278f;
            double B2 = 0.365f;
            double D1 = 0.267f;
            double D2 = 0.445f;

            double N = 0.028f;
            double M = 0.147f;

            double lookup[][];
            int precalc_len;

            double sigma(double x, double a, double alpha) {
                double inner = -4.0f / alpha * (x - a);
                return 1.0f / (1.0f + Math.exp(inner));
//                if(inner < -10.0f) {
//                    return 0.0f;
//                } else if(inner > 10.0f) {
//                    return 1.0f;
//                } else {
//                    return 1.0f / (1.0f + (double) Math.exp(inner));
//                }
            }

            double sigma2(double x, double a, double b) {
                return sigma(x, a, N) * (1.0f - sigma(x, b, N));
            }

            double lerp(double a, double b, double t) {
                return (1.0f - t) * a + t * b;
            }

            double s(double n, double m) {
                double alive = sigma(m, 0.5f, M);
                return sigma2(n, lerp(B1, D1, alive), lerp(B2, D2, alive));
            }

            void precalculate(int len) {
                Log.d(TAG, "Precalculating rules");
                precalc_len = len;
                double dlen = (double)(len );
                lookup = new double[len][len];
                for(int n=0; n<len; n++) {
                    for(int m=0; m<len; m++) {
                        lookup[n][m] = s(n / dlen, m / dlen);
                    }
                }
            }

            void s_fast(double dest[], double n_arr[], double m_arr[], double dt) {
                for(int i=0; i<n_arr.length; i++) {
                    int n_ind = (int)(n_arr[i] * precalc_len + 0.5);
                    int m_ind = (int)(m_arr[i] * precalc_len + 0.5);
                    // Constrain just in case
                    if(n_ind >= precalc_len) n_ind = precalc_len - 1;
                    if(n_ind < 0) n_ind = 0;
                    if(m_ind >= precalc_len) m_ind = precalc_len - 1;
                    if(m_ind < 0) m_ind = 0;

                    dest[i] = lookup[n_ind][m_ind];
                }
            }
        }

        private class SmoothTimestepRules extends Rules {
            double B1 = 0.254f;
            double B2 = 0.340f;
            double D1 = 0.312f;
            double D2 = 0.518f;
//
//            double N = 0.028f;
//            double M = 0.147f;
//
//            double lookup[][];
//            int precalc_len;

            double hard(double x, double a) {
                return x > a ? 1.0f : 0.0f;
            }

            double sigma(double x, double a, double alpha) {
                return x * (1.0 - hard(alpha, 0.5)) + a * hard(alpha, 0.5);
            }

            double linear(double x, double a, double ea) {
                double val = (x - a) / ea + 0.5;
                return  Math.min(Math.max(val, 0.0), 1.0);
            }

            double sigma2(double x, double a, double b) {
                return linear(x, a, N) * (1.0 - linear(x, b, N));
            }

            double s(double n, double m) {
                return sigma(sigma2(n, B1, D1), sigma2(n, B2, D2), m);
            }

            void s_fast(double dest[], double n_arr[], double m_arr[], double dt) {
                for(int i=0; i<n_arr.length; i++) {
                    int n_ind = (int)(n_arr[i] * precalc_len + 0.5);
                    int m_ind = (int)(m_arr[i] * precalc_len + 0.5);
                    // Constrain just in case
                    if(n_ind >= precalc_len) n_ind = precalc_len - 1;
                    if(n_ind < 0) n_ind = 0;
                    if(m_ind >= precalc_len) m_ind = precalc_len - 1;
                    if(m_ind < 0) m_ind = 0;

                    double s = lookup[n_ind][m_ind];
//                    double m = m_arr[i];
                    double f = dest[i];
                    double v =  f + dt * (s - f);
                    dest[i] = Math.min(Math.max(v, 0.0), 1.0);

                }
            }
        }

        private class Multipliers {
            int height;
            int width;
            double inner_r;
            double outer_r;
            double M[];
            double N[];
//            Logistic2d m;
//            Logistic2d n;

            private String TAG = "Multipliers";

            public Multipliers(int height, int width, double inner_r, double outer_r) {
                this.height = height;
                this.width = width;
                this.inner_r = inner_r;
                this.outer_r = outer_r;

                Log.d(TAG, "Creating logistics");
                Logistic2d m = new Logistic2d(inner_r);
                Logistic2d n = new Logistic2d(outer_r);

                double _M[] = new double[width*height];
                double _N[] = new double[width*height];
                M = new double[height * (width/2 + 1) * 2];
                N = new double[height * (width/2 + 1) * 2];

                double inner_sum = m.sum;
                double annulus_sum = n.sum - m.sum;
                for (int r = 0; r < height; r++) {
                    for (int c = 0; c < width; c++) {
                        int i = r * width + c;
                        _M[i] = m.grid[i] / inner_sum;
                        _N[i] = (n.grid[i] - m.grid[i]) / annulus_sum;
                    }
                }

//                // Gaussian blue test
//                for (int r = 0; r < height; r++) {
//                    for (int c = 0; c < width; c++) {
//                        int i = r * width + c;
//                        _M[i] = 0;
//                        _N[i] = 0;
//                    }
//                }
//                int size = width * height;
//                _M[0] = 0.5;
//                _M[width] = 0.5;

//                _M[0] = 4.0/16.0;
//                _M[1] = 2.0/16.0;
//                _M[width] = 2.0/16.0;
//                _M[width + 1] = 1.0/16.0;
//                _M[width - 1] = 2.0/16.0;
//                _M[2*width - 1] = 1.0/16.0;
//                _M[size - width] = 2.0/16.0;
//                _M[size - width + 1] = 1.0/16.0;
//                _M[size - 1] = 1.0/16.0;


                Log.d(TAG, "Putting _M");
                doublepointer_in_forward.put(_M);
                Log.d(TAG, "Executing forward");
                fftw3.fftw_execute(plan_forward);
                Log.d(TAG, "Getting M");
                doublepointer_out_forward.get(M);

                Log.d(TAG, "Doing it for N");
                doublepointer_in_forward.put(_N);
                fftw3.fftw_execute(plan_forward);
                doublepointer_out_forward.get(N);


            }

            private class Logistic2d {
                double grid[];
                double sum;

                public Logistic2d(double radius) {
                    sum = 0;
                    grid = new double[width*height];
                    double logres = Math.log(Math.min(width, height)) / Math.log(2);
                    for (int r = 0; r < height; r++) {
                        for (int c = 0; c < width; c++) {
                            double rr = ((r + height / 2) % height) - height / 2;
                            double cc = ((c + width / 2) % width) - width / 2;
                            double dist = Math.sqrt(rr * rr + cc * cc) - radius;
                            double logistic = 1.0f / (1.0f + Math.exp(logres * dist));
                            sum += logistic;
                            grid[r * width + c] = logistic;
                        }
                    }
                }
            }


        }



        private final Handler handler = new Handler();

        private final Runnable drawRunner = new Runnable() {
            @Override
            public void run() {
//                Log.d(TAG, "Running");
                draw();
            }
        };
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            String val;
            if(key.equals("color_scaling")) {
                val = String.valueOf(prefs.getInt(key, 50));
                Log.d(TAG, "Caught pref " + key + " changing to " + val);
                color_scaling = prefs.getInt("color_scaling", 50);
            }
            else if (key.equals("frame_delay")) {
                frame_delay = Integer.parseInt(prefs.getString("frame_delay", "1000"));
            }
            else if(key.equals("color_map_choice")) {
                cmap = ColorMap.getColorMap(prefs.getString(key, "viridis"));
            }
            else if(key.equals("smooth_timestepping")) {
                reinit(true);
            }
            else {
                val = prefs.getString(key, "undef");
                Log.d(TAG, "Caught pref " + key + " changing to " + val);
                // Sometimes we see changes twice. Watch out for that.
                if(! last_pref_value.equals(val)) {
                    last_pref_value = val;
                    reinit(true);
                }
            }
        }

        private void add_speckles() {
            double intensity = 1.0;
//            float count = 200f * (width * height) / (128 * 128);
//            count *= (7.0f / inner_radius) * (7.0f / inner_radius);
            double count = width * height / (outer_radius * 2) / (outer_radius * 2);
            int icount = (int)count;
//            icount = 10;

            // Clear first
            for(int i=0; i<(width * height); i++) {
                field[i] = 0.0;
            }
            Random random = new Random();
            int radius = (int)outer_radius;
            for(int i=0; i<icount; i++) {
                int r = random.nextInt(height - radius);
                int c = random.nextInt(width - radius);
                for(int rr=0; rr<radius; rr++) {
                    for(int cc=0; cc<radius; cc++) {
                        int index = width * (r + rr) + c + cc;
                        field[index] = intensity;
                    }
                }
            }
            dead_count = 0;
        }

        public void reinit(boolean force) {
            frame_delay = Integer.parseInt(prefs.getString("frame_delay", "1000"));
            scale = Integer.parseInt(prefs.getString("scale", "4"));
            inner_radius = Integer.parseInt(prefs.getString("inner_radius", "7"));
            dt = Double.parseDouble(prefs.getString("timestep", "0.2"));
            Boolean is_smooth_timestepping = prefs.getBoolean("smooth_timestepping", false);
            if (!is_smooth_timestepping) {
                dt = -1;
            }
            outer_radius = inner_radius*3;
            width = actual_width / scale;
            height = actual_height / scale;
            if(force || last_width != width || last_height != height || last_dt != dt) {
                delete_plans();
                make_plans();
                field = new double[width * height];
                pixels = new int[width * height];
                multipliers = new Multipliers(height, width, inner_radius, outer_radius);
                if (is_smooth_timestepping) {
                    rules = new SmoothTimestepRules();
                }
                else {
                    rules = new Rules();
                }
                rules.precalculate(512);
                add_speckles();
                last_width = width;
                last_height = height;
                last_dt = dt;
                bitmap = new Bitmap[2];
                bitmap[0] = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap[1] = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            }
            cmap = ColorMap.getColorMap(prefs.getString("color_map_choice", "viridis"));
            color_scaling = prefs.getInt("color_scaling", 50);

        }

//        private void step() {
//            int csize = height*width*2;
//            int size = width*height;
//            double in_buffer[] = new double[csize];
//            double out_buffer[] = new double[csize];
//            DoublePointer in_dp = new DoublePointer(csize);
//            DoublePointer out_dp = new DoublePointer(csize);
//            fftw3.fftw_plan plan_forward = fftw3.fftw_plan_dft_2d(height, width, in_dp, out_dp, (int)fftw3.FFTW_FORWARD, (int)fftw3.FFTW_ESTIMATE);
////            fftw3.fftw_plan plan_reverse = fftw3.fftw_plan_dft_2d(height, width, in_dp, out_dp, (int)fftw3.FFTW_BACKWARD, (int)fftw3.FFTW_ESTIMATE);
//            for(int i=0; i<csize; i++) {
//                in_buffer[i] = 0;
//            }
//            in_buffer[0] = 0.5;
//            in_buffer[width*2] = 0.5;
//            in_dp.put(in_buffer);
//            fftw3.fftw_execute(plan_forward);
//            out_dp.get(out_buffer);
//
//            for(int r=0; r<height; r++) {
//                for(int c=0; c<width; c++) {
//                    field[r*width + c] = out_buffer[(r*width + c) * 2 + 0];
////                    field[r*width + c] = m_buffer[r*width + c];
//                }
//            }
//            normalize(field);
//
//
//        }

//        private void step() {
//            // Test dft_r2c
//            int csize = height*(width/2 + 1)*2;
//            int size = width*height;
//            double in_buffer[] = new double[size];
//            double out_buffer[] = new double[csize];
//            DoublePointer in_dp = new DoublePointer(size);
//            DoublePointer out_dp = new DoublePointer(csize);
//            fftw3.fftw_plan plan_forward = fftw3.fftw_plan_dft_r2c_2d(height, width, in_dp, out_dp, (int)fftw3.FFTW_ESTIMATE);
////            fftw3.fftw_plan plan_reverse = fftw3.fftw_plan_dft_2d(height, width, in_dp, out_dp, (int)fftw3.FFTW_BACKWARD, (int)fftw3.FFTW_ESTIMATE);
//            for(int i=0; i<size; i++) {
//                in_buffer[i] = 0;
//            }
//            in_buffer[0] = 0.5;
//            in_buffer[width] = 0.5;
//            in_dp.put(in_buffer);
//            fftw3.fftw_execute(plan_forward);
//            out_dp.get(out_buffer);
//
//            for(int r=0; r<height; r++) {
//                for(int c=0; c<(width/2+1); c++) {
//                    field[r*width + c] = out_buffer[(r*(width/2+1) + c) * 2 + 0];
////                    field[r*width + c] = m_buffer[r*width + c];
//                }
//            }
//            normalize(field);
//
//
//        }

        private void step() {
            // Actual step
            final int csize = height * (width/2 + 1) * 2;
            final int size = height * width;
            final double field_[] = new double[csize];

            final double m_buffer_[] = new double[csize];
            final double m_buffer[] = new double[size];
            final double n_buffer_[] = new double[csize];
            final double n_buffer[] = new double[size];

            doublepointer_in_forward.put(field);
            fftw3.fftw_execute(plan_forward);
            doublepointer_out_forward.get(field_);

            final double norm = size;

            // Doing the second FFT in a new thread is faster in the emulator
            // But seemingly slightly slower IRL
            Thread fft_thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    for(int i=0; i<csize; i+=2) {
                        double fr, fi, kr, ki;
                        fr = field_[i];
                        fi = field_[i + 1];
                        kr = multipliers.M[i];
                        ki = multipliers.M[i + 1];
                        m_buffer_[i] = (fr * kr - fi * ki) / norm;
                        m_buffer_[i + 1] = (fr * ki + fi * kr) / norm;
                    }

                    doublepointer_in_reverse_m.put(m_buffer_);
                    fftw3.fftw_execute(plan_reverse);
                    doublepointer_out_reverse_m.get(m_buffer);
                }
            });

            fft_thread.start();



            for(int i=0; i<csize; i+=2) {
                double fr, fi, kr, ki;
                fr = field_[i];
                fi = field_[i + 1];

                kr = multipliers.N[i];
                ki = multipliers.N[i + 1];
                n_buffer_[i] = (fr * kr - fi * ki) / norm;
                n_buffer_[i + 1] = (fr * ki + fi * kr) / norm;

//                kr = multipliers.M[i];
//                ki = multipliers.M[i + 1];
//                m_buffer_[i] = (fr * kr - fi * ki) / norm;
//                m_buffer_[i + 1] = (fr * ki + fi * kr) / norm;

            }

//            doublepointer_in_reverse_m.put(m_buffer_);
//            fftw3.fftw_execute(plan_reverse);
//            doublepointer_out_reverse_m.get(m_buffer);

            doublepointer_in_reverse_n.put(n_buffer_);
            fftw3.fftw_execute_dft_c2r(plan_reverse, doublepointer_in_reverse_n, doublepointer_out_reverse_n);
            doublepointer_out_reverse_n.get(n_buffer);

            try {
                fft_thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }


            rules.s_fast(field, n_buffer, m_buffer, dt);

        }

        public void normalize(double arr[]) {
            double min = arr[0];
            double max = min;
            for(int i=0; i<arr.length; i++) {
                if(arr[i] < min) min = arr[i];
                if(arr[i] > max) max = arr[i];
            }
            for(int i=0; i<arr.length; i++) {
                arr[i] = (arr[i] - min) / (max - min);
            }

        }

        private double sigmoid_tuneable(double x, int ik) {
            double k = (ik - 50) / 50.1;
            double offset = 0.0;
            if(x > 0.5) {
                x -= 0.5;
                k *= -1.0;
                offset = 0.5;
            }
            return (k*x - x) / (4*k*x - k - 1) + offset;
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setDither(true);
            actual_width = getDesiredMinimumWidth();
            actual_height = getDesiredMinimumHeight();
            Log.d(TAG, "Surface created " + width + " x " + height);
            PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
            prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            prefs.registerOnSharedPreferenceChangeListener(this);
            reinit(false);
            draw();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            this.actual_width = width;
            this.actual_height = height;
            reinit(false);
            Log.d(TAG, "Surface changed " + width + " x " + height);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "Surface destroyed");
            super.onSurfaceDestroyed(holder);
            visible = false;
            prefs.unregisterOnSharedPreferenceChangeListener(this);
            delete_plans();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;

            Log.d(TAG, "Visibility changed to " + visible);
            if(visible) {
                add_speckles();
                draw();
            } else {
                handler.removeCallbacks(drawRunner);
            }
        }

        private void do_sigmoid_tuneable(double dest[], double src[], int color_scaling) {
            for(int i=0; i<src.length; i++) {
                dest[i] = sigmoid_tuneable(src[i], color_scaling);
            }
        }

        private void do_cmap(int dest[], double src[]) {
            for(int i=0; i<src.length; i++) {
                dest[i] = cmap.get_fast(src[i]);
            }
        }

        void step_and_update() {

            step();
            if(color_scaling == 50) {
                do_cmap(pixels, field);
            }
            else {
                double tmp[] = new double[width * height];
                do_sigmoid_tuneable(tmp, field, color_scaling);
                do_cmap(pixels, tmp);
            }
            bitmap[bitmap_index].setPixels(pixels, 0, width, 0, 0, width, height);

            double sum = 0.0;
            for(int i=0; i<width*height; i++) {
                sum += field[i];
            }
//            Log.d(TAG, "Sum is: " + String.valueOf((int)sum));
            if(sum < 10) {
                Log.d(TAG, "Grid is dead. Reseeding");
                add_speckles();
            }
            else if(sum < 900*inner_radius*inner_radius/49) {
                // For an inner_radius of 7, a single glider has a sum around 800
                // If we only have a single glider for a while, resize
                dead_count ++;
                if(dead_count > 100) {
                    Log.d(TAG, "Single glider has been around too long");
                    add_speckles();
                }
            }

        }

        private void draw() {
            if (visible) {
                long etime = System.currentTimeMillis();

                Thread stepper_thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        step_and_update();
                    }
                });

                stepper_thread.start();
                SurfaceHolder holder = getSurfaceHolder();
                Canvas canvas = holder.lockCanvas();


                canvas.drawBitmap(bitmap[1 - bitmap_index], new Rect(0, 0, width, height), new Rect(0, 0, width*scale, height*scale), paint);
                holder.unlockCanvasAndPost(canvas);

                try {
                    stepper_thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    handler.removeCallbacks(drawRunner);
                }

                bitmap_index = 1 - bitmap_index;

                frame_millis_sum += System.currentTimeMillis() - etime;
                if(frame_counter++ % 32 == 0) {
                    long avg = frame_millis_sum / 32;
                    long fps = avg>0L ? 1000 / avg : 0L;
                    Log.d(TAG, "Avg time per frame: " + avg + ", fps: " + fps);
                    frame_millis_sum = 0;
                }


            } else {
                Log.d(TAG, "Invisible so not drawing");
            }

            handler.removeCallbacks(drawRunner);
            if(visible) {
//                Log.d(TAG, "Setting delay to " + frame_delay);
                handler.postDelayed(drawRunner, frame_delay);
            }
        }
    }
}
