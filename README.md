# RetrofitUtils
RetrofitUtils 快速集成Retrofit+okhtp，不使用rxjava


调用方式：
        Configuration.Builder builder = new Configuration.Builder();
        builder.baseUrl("https://www.baidu.com").context(this);
        
        retrofitUtils = new RetrofitUtils(builder.build());
        retrofitUtils.getFullPath("https://www.baidu.com",null, new HttpCallback<String>() {
            @Override
            public void onError(String code, String message) {

            }

            @Override
            public void onResponse(String response) {

            }
        });
