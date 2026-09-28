#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 InverseModelViewMat;

out vec4 vertexColor;
out vec2 texCoord0;
out float viewRelY;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;

    vertexColor = Color;
    texCoord0 = UV0;

    vec4 localPos = InverseModelViewMat * viewPos;
    viewRelY = localPos.y;
}