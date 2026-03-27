{
  description = "Public Transit Portal dev environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };
      in
      {
        devShells.default = pkgs.mkShell {
          packages = [
            pkgs.nodejs # includes npm and npx
            pkgs.babashka
          ];

          shellHook = ''
            echo "Public Transit Portal dev shell"
            echo "node: $(node --version)"
            echo "npm:  $(npm --version)"
            echo "npx:  $(npx --version)"
            echo "bb:   $(bb --version)"
            echo ""
            echo "Run 'bb dev' to start the dev server (port 8080) + nREPL (port 9000)"
          '';
        };
      }
    );
}
