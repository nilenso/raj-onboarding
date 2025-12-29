{
  description = "ProjectNIL development environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
    flake-parts.url = "github:hercules-ci/flake-parts";
  };

  outputs = inputs@{ flake-parts, ... }:
    flake-parts.lib.mkFlake { inherit inputs; } {
      systems = [ "x86_64-linux" "aarch64-linux" "x86_64-darwin" "aarch64-darwin" ];

      perSystem = { pkgs, ... }: 
      let
        # Fetch assemblyscript-json from npm registry
        assemblyscript-json = pkgs.fetchzip {
          url = "https://registry.npmjs.org/assemblyscript-json/-/assemblyscript-json-1.1.0.tgz";
          hash = "sha256-uL9bRjcnhbLoILhHwMCE5dJPDh/1hwWyFLl+XpAdWrA=";
        };
      in
      {
        devShells.default = pkgs.mkShell {
          packages = [
            pkgs.openjdk25
            pkgs.assemblyscript
          ];

          shellHook = ''
            echo "ProjectNIL devshell"
            echo "Java: $(java --version | head -1)"
            
            # Make assemblyscript-json available to asc compiler
            export NODE_PATH="${assemblyscript-json}/..:$NODE_PATH"
          '';
        };
      };
    };
}
