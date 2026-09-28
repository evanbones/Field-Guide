#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D Sampler0;

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 texCoord0;
layout(location = 2) in float viewRelY;
layout(location = 3) in float scanLimitY;
layout(location = 4) in vec4 scanColor;

layout(location = 0) out vec4 fragColor;

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