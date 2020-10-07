function [y]=VMFMeanDirDensity(x, k, p)

% This is the tangent direction density of VMF distribution. See the
% SphereDistributionsRand.pdf file for detail description and formulas.
%
% Usage:
%   [y]=VMFMeanDirDensity(x, k, p);
%
% Inputs:
%   x: The tangent direction value. should be in [-1 1].
%
%   k: The kappa parameter of the VMF distribution.
%
%   p: The dimension of the VMF distribution.
%
% Outputs:
%   y : The density value of the VMF tangent density.
%
% Function is written by Yu-Hui Chen, University of Michigan
% Contact E-mail: yuhuic@umich.edu
%
if(any((x<-1) | (x>1)))
    error('Input of x should be within -1~1');
end
Coeff = (k/2)^(p/2-1) * (gamma((p-1)/2)*gamma(1/2)*besseli(p/2-1,k))^(-1);
y = Coeff * exp(k*x).*(1-x.^2).^((p-3)/2);

end