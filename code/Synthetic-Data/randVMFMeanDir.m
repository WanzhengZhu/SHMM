function [t] = randVMFMeanDir(N, k, p)

% This function generate random samples from the tangent direction of von
% Mises-Fisher distribution using rejection sampling. The density of is
% described in VMFMeanDirDensity function. See the
% SphereDistributionsRand.pdf file for detail description and formulas.
%
% Usage:
%   [t] = randVMFMeanDir(N, k, p);
%
% Inputs:
%   N: The number of samples one wants to generate.
%
%   k: The kappa parameter of the VMF distribution.
%
%   p: The dimension of the VMF distribution.
%
% Outputs:
%   t : A N x 1 vector which are the random samples from the VMF's tangent
%   distribution.
%
% Function is written by Yu-Hui Chen, University of Michigan
% Contact E-mail: yuhuic@umich.edu
%

min_thresh = 1/(5*N);

xx = -1:0.000001:1;
yy = VMFMeanDirDensity(xx, k, p);
cumyy = cumsum(yy)*(xx(2)-xx(1));

leftBound = xx(find(cumyy>min_thresh,1));

%%% Fin the left bound
xx = linspace(leftBound, 1, 1000);
yy = VMFMeanDirDensity(xx, k, p);

M = max(yy);
t = zeros(N,1);
for i=1:N
    while(1)
        x = rand*(1-leftBound)+leftBound;
        h = VMFMeanDirDensity(x, k, p);
        draw = rand*M;
        if(draw<=h)
            break;
        end
    end
    t(i) = x;
end

end