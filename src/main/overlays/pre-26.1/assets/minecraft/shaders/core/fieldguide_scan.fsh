#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float ScanLimitY;

in vec4 vertexColor;
in vec2 texCoord0;
in float viewRelY;

out vec4 fragColor;

void main() {
    if (viewRelY > ScanLimitY) {
        discard;
    }

    vec4 texSample = texture(Sampler0, texCoord0);
    if (texSample.a < 0.1) {
        discard;
    }

    vec4 baseColor = vec4(vertexColor.rgb, vertexColor.a * texSample.a);
    float outlineThickness = 0.05;

    if (viewRelY > ScanLimitY - outlineThickness) {
        fragColor = vec4(baseColor.rgb * 3.0, texSample.a) * ColorModulator;
    } else {
        fragColor = baseColor * ColorModulator;
    }
}