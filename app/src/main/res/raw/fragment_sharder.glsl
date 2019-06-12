varying highp vec2 vTexCoord;
uniform sampler2D sTexture;
uniform highp float widthOfset;
uniform highp float heightOfset;
uniform highp float gaussianWeights[961];
uniform highp int blurRadius;
uniform highp int blurSigma;
void main() {
    if(blurSigma == 0 || blurRadius == 0){
        gl_FragColor = texture2D(sTexture,vTexCoord);
    }else{
        highp vec2 offset = vec2(widthOfset,heightOfset);
        highp vec4 sum = vec4(0.0);
        highp int x = 0;
        for (int i = -blurRadius; i <= blurRadius; i++) {
            for (int j = -blurRadius; j <= blurRadius; j++) {
                highp float weight = gaussianWeights[x];
                sum += (texture2D(sTexture, vTexCoord+offset*vec2(i,j))*weight);
                x++;
            }
        }
        gl_FragColor = sum;
    }

}