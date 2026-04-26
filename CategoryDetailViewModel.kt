clc;
clear;

%% TWO-PHASE METHOD (without linprog)

% Variables:
% [x1 x2 s1 a1 a2 s3]

%% -------------------------------------------------
% PHASE I : Minimize W = a1 + a2
%% -------------------------------------------------

C1 = [0 0 0 1 1 0];

A = [
    1 1 -1 1 0 0 4;
    1 2  0 0 1 0 6;
    2 1  0 0 0 1 8
];

BV = [4 5 6];   % a1, a2, s3 are initial basic vars

[m,n1] = size(A);
n = n1 - 1;     % excluding RHS

fprintf('=========== PHASE I ===========\n');

while true
    
    % Compute Zj
    Zj = zeros(1,n+1);
    for i = 1:m
        Zj = Zj + C1(BV(i))*A(i,:);
    end
    
    % Compute Cj - Zj
    CJ_ZJ = [C1 0] - Zj;
    
    disp('Current Tableau:')
    disp(A)
    
    disp('Cj - Zj:')
    disp(CJ_ZJ)
    
    % Check optimality
    [minval,pivot_col] = min(CJ_ZJ(1:n));
    
    if minval >= 0
        break;
    end
    
    % Ratio test
    ratio = inf(m,1);
    for i = 1:m
        if A(i,pivot_col) > 0
            ratio(i) = A(i,end)/A(i,pivot_col);
        end
    end
    
    [~,pivot_row] = min(ratio);
    
    % Update basis
    BV(pivot_row) = pivot_col;
    
    % Pivot
    pivot = A(pivot_row,pivot_col);
    A(pivot_row,:) = A(pivot_row,:) / pivot;
    
    for i = 1:m
        if i ~= pivot_row
            A(i,:) = A(i,:) - A(i,pivot_col)*A(pivot_row,:);
        end
    end
end

W = Zj(end);

fprintf('Minimum W = %f\n',W);

if abs(W) > 1e-6
    fprintf('Problem is infeasible.\n');
    return;
end

%% -------------------------------------------------
% Remove artificial variable columns a1,a2
%% -------------------------------------------------

A(:,[4 5]) = [];

% New variables:
% [x1 x2 s1 s3]

C2 = [2 3 0 0];

% Update basis indices after removing cols
for i = 1:length(BV)
    if BV(i) > 5
        BV(i) = BV(i) - 2;
    elseif BV(i) > 3
        BV(i) = BV(i) - 1;
    end
end

n = size(A,2) - 1;

fprintf('\n=========== PHASE II ===========\n');

while true
    
    % Compute Zj
    Zj = zeros(1,n+1);
    for i = 1:m
        Zj = Zj + C2(BV(i))*A(i,:);
    end
    
    % Compute Cj - Zj
    CJ_ZJ = [C2 0] - Zj;
    
    disp('Current Tableau:')
    disp(A)
    
    disp('Cj - Zj:')
    disp(CJ_ZJ)
    
    % Check optimality
    [minval,pivot_col] = min(CJ_ZJ(1:n));
    
    if minval >= 0
        break;
    end
    
    % Ratio test
    ratio = inf(m,1);
    for i = 1:m
        if A(i,pivot_col) > 0
            ratio(i) = A(i,end)/A(i,pivot_col);
        end
    end
    
    [~,pivot_row] = min(ratio);
    
    % Update basis
    BV(pivot_row) = pivot_col;
    
    % Pivot
    pivot = A(pivot_row,pivot_col);
    A(pivot_row,:) = A(pivot_row,:) / pivot;
    
    for i = 1:m
        if i ~= pivot_row
            A(i,:) = A(i,:) - A(i,pivot_col)*A(pivot_row,:);
        end
    end
end

%% Final Solution

x = zeros(n,1);

for i = 1:m
    x(BV(i)) = A(i,end);
end

Z = C2*x;

fprintf('\n=========== FINAL ANSWER ===========\n');
fprintf('x1 = %f\n',x(1));
fprintf('x2 = %f\n',x(2));
fprintf('Minimum Z = %f\n',Z);
