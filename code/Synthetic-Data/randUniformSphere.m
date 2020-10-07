function [RandSphere] = randUniformSphere(N, p)

% This function generate the random samples uniformly on a dim-dimensional
% sphere.
%
% Usage:
%   [RandSphere] = randUniformSphere(N, p);
%
% Inputs:
%   N: The number of samples one wants to generate.
%
%   p : The dimension of the generated samples. 
%       p==2 => samples are on a unit circle.
%       p==3 => samples are on a unit sphere.
%
% Outputs:
%   RandSphere : A N x dim matrix which are the N random samples generated
%       on the unit p-dimensional sphere. Each sample has unit length,
%       eq: norm(RandSphere(x,:))==1 for any 1<=x<=N.
%
% Function is written by Yu-Hui Chen, University of Michigan
% Contact E-mail: yuhuic@umich.edu
%

randNorm = normrnd(zeros(N,p), 1, [N, p]);
RandSphere = zeros(N,p);
for r=1:N
    RandSphere(r,:) = randNorm(r,:)./norm(randNorm(r,:));
end

end