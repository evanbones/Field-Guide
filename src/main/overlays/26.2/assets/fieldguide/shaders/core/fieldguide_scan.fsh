#version 150

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;
in float viewRelY;
in float scanLimitY;
in vec4 scanColor;

out vec4 fragColor;

void main() {
    if (viewRelY > scanLimitY) {
        discard;
    }

    vec4 texSample = texture(Sampler0, texCoord0);
    if (texSample.a < 0.1) {
        discard;
    }

    vec4 baseColor = vec4(vertexColor.rgb * scanColor.rgb, vertexColor.a * texSample.a * scanColor.a);

    float outlineThickness = 0.05;

    if (viewRelY > scanLimitY - outlineThickness) {
        fragColor = vec4(baseColor.rgb * 3.0, baseColor.a);
    } else {
        fragColor = baseColor;
    }
}